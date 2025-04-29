package com.example.prototype

// ---------- import ----------
import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

// ---------- end import ----------

class ProtoWearSensorService :
    Service(),
    SensorEventListener {
    companion object {
        private const val TAG = "ProtoWearSensorService"

        // 센서 주기
        private const val ACC_GYRO_SAMPLING_RATE = SensorManager.SENSOR_DELAY_GAME // ≈25 Hz
        private const val BARO_SAMPLING_RATE = SensorManager.SENSOR_DELAY_UI // ≈10 Hz
        private const val STEP_SAMPLING_RATE = SensorManager.SENSOR_DELAY_NORMAL // ≈5 Hz

        // 패킷 전송 주기
        private const val PACKET_INTERVAL_MS = 250L

        // Data-Layer 경로
        private const val SENSOR_DATA_PATH = "/sensor_data"
        private const val ACTIVITY_UPDATE_PATH = "/activity_update"

        // ActivityRecognition 브로드캐스트 액션
        private const val ACTION_ACTIVITY_RECOG = "com.example.prototype.ACTIVITY_RECOGNITION"
    }

    // ---------- 시스템 서비스 ----------
    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock

    // ---------- Wearable(노드·메시지) ----------
    private lateinit var nodeClient: NodeClient
    private lateinit var messageClient: MessageClient
    private var connectedNode: Node? = null

    // ---------- Activity Recognition ----------
    private lateinit var activityClient: ActivityRecognitionClient
    private lateinit var pendingActivityPI: PendingIntent

    private val activityReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                val result = ActivityRecognitionResult.extractResult(intent ?: return) ?: return
                val most = result.mostProbableActivity ?: return
                Log.d(
                    TAG,
                    "ActivityRecognition received type=${most.type} confidence=${most.confidence}"
                )
                sendActivityUpdate(most.type)
            }
        }

    // ---------- 센서 레퍼런스 ----------
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var barometer: Sensor? = null
    private var stepDetector: Sensor? = null

    // ---------- 센서 데이터 버퍼 ----------
    private var timestamp: Long = 0L
    private var ax = 0f
    private var ay = 0f
    private var az = 0f
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private var pressure = 0f
    private var stepFlag = 0f

    // ---------- 코루틴 ----------
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ─────────────────────────────────────────────
    // Service lifecycle
    // ─────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        // 1) Wake-lock
        wakeLock =
            (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ProtoType:WakeLock")
                .apply { acquire() }

        // 2) Wearable 클라이언트
        nodeClient = Wearable.getNodeClient(this)
        messageClient = Wearable.getMessageClient(this)

        // 3) Activity Recognition 설정
        activityClient = ActivityRecognition.getClient(this) // ★
        val arIntent = Intent(ACTION_ACTIVITY_RECOG).setPackage(packageName) // ★ 명시적
        pendingActivityPI =
            PendingIntent.getBroadcast(
                this,
                0,
                arIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val arFilter = IntentFilter(ACTION_ACTIVITY_RECOG)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(activityReceiver, arFilter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(activityReceiver, arFilter)
        }

        // -------- Permission check --------
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "ACTIVITY_RECOGNITION permission not granted. Stopping service.")
            stopSelf()
            return
        }

        activityClient
            .requestActivityUpdates(3_000L, pendingActivityPI) // 3 s 
            .addOnSuccessListener { Log.d(TAG, "requestActivityUpdates success") }
            .addOnFailureListener { e -> Log.e(TAG, "requestActivityUpdates fail", e) }

        // 4) SensorManager & 센서 등록
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        // Register sensors only if permission is still granted (may have been revoked)
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            accelerometer?.let { sensorManager.registerListener(this, it, ACC_GYRO_SAMPLING_RATE) }
            gyroscope?.let { sensorManager.registerListener(this, it, ACC_GYRO_SAMPLING_RATE) }
            barometer?.let { sensorManager.registerListener(this, it, BARO_SAMPLING_RATE) }
            stepDetector?.let { sensorManager.registerListener(this, it, STEP_SAMPLING_RATE) }
        } else {
            Log.w(TAG, "Permission lost before sensor registration")
        }

        // 5) DataLayer 전송 태스크 & 노드 탐색
        startPacketLoop()
        findConnectedNode()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        accelerometer?.let { sensorManager.registerListener(this, it, ACC_GYRO_SAMPLING_RATE) }
        gyroscope?.let { sensorManager.registerListener(this, it, ACC_GYRO_SAMPLING_RATE) }
        barometer?.let { sensorManager.registerListener(this, it, BARO_SAMPLING_RATE) }
        stepDetector?.let { sensorManager.registerListener(this, it, STEP_SAMPLING_RATE) }

        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)

        try {
            activityClient.removeActivityUpdates(pendingActivityPI)
            unregisterReceiver(activityReceiver)
        } catch (_: Exception) {
        }

        scope.cancel()

        if (wakeLock.isHeld) wakeLock.release()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─────────────────────────────────────────────
    // Sensor callbacks
    // ─────────────────────────────────────────────

    override fun onSensorChanged(e: SensorEvent?) {
        when (e?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                ax = e.values[0]
                ay = e.values[1]
                az = e.values[2]
                timestamp = e.timestamp
            }
            Sensor.TYPE_GYROSCOPE -> {
                gx = e.values[0]
                gy = e.values[1]
                gz = e.values[2]
            }
            Sensor.TYPE_PRESSURE -> {
                pressure = e.values[0] // hPa
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                stepFlag = 1f
            }
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit

    // ─────────────────────────────────────────────
    // Data-Layer helpers
    // ─────────────────────────────────────────────

    private fun startPacketLoop() =
        scope.launch {
            while (isActive) {
                sendSensorPacket()
                delay(PACKET_INTERVAL_MS)
            }
        }

    private fun findConnectedNode() =
        scope.launch {
            try {
                connectedNode =
                    Tasks
                        .await(nodeClient.connectedNodes, 5, TimeUnit.SECONDS)
                        .firstOrNull()
                Log.d(TAG, "Connected node: ${connectedNode?.displayName ?: "None"}")
            } catch (e: Exception) {
                Log.e(TAG, "findConnectedNode error", e)
            }
        }

    private fun sendSensorPacket() {
        val nodeId = connectedNode?.id ?: return

        val buf =
            ByteBuffer.allocate(9 * 8).apply {
                putDouble(timestamp.toDouble())
                putDouble(ax.toDouble())
                putDouble(ay.toDouble())
                putDouble(az.toDouble())
                putDouble(gx.toDouble())
                putDouble(gy.toDouble())
                putDouble(gz.toDouble())
                putDouble(pressure.toDouble())
                putDouble(stepFlag.toDouble())
            }

        messageClient
            .sendMessage(nodeId, SENSOR_DATA_PATH, buf.array())
            .addOnSuccessListener {
                stepFlag = 0f // reset after sending
                // Success
            }.addOnFailureListener { e ->
                Log.e(TAG, "sendSensorPacket error", e)
                findConnectedNode()
            }
    }

    private fun sendActivityUpdate(type: Int) {
        val nodeId = connectedNode?.id ?: return
        val bytes = ByteBuffer.allocate(4).putInt(type).array()
        Log.d(TAG, "sendActivityUpdate to node=$nodeId type=$type")
        messageClient
            .sendMessage(nodeId, ACTIVITY_UPDATE_PATH, bytes)
            .addOnFailureListener { e -> Log.e(TAG, "sendActivityUpdate error", e) }
    }
}