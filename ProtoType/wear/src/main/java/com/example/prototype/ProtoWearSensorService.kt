// File: com/example/prototype/ProtoWearSensorService.kt
package com.example.prototype

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
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

@Suppress("UnspecifiedRegisterReceiverFlag", "MissingPermission")
class ProtoWearSensorService :
    Service(),
    SensorEventListener {
    companion object {
        private const val TAG = "ProtoWearSensorService"

        // 샘플링 주기
        private const val ACC_GYRO_SAMPLING_RATE = SensorManager.SENSOR_DELAY_GAME
        private const val BARO_SAMPLING_RATE = SensorManager.SENSOR_DELAY_UI
        private const val STEP_SAMPLING_RATE = SensorManager.SENSOR_DELAY_NORMAL

        // 전송 주기
        private const val PACKET_INTERVAL_MS = 250L

        // Wear Data Layer 경로
        private const val SENSOR_DATA_PATH = "/sensor_data"
        private const val ACTIVITY_UPDATE_PATH = "/activity_update"

        // 브로드캐스트 액션
        private const val ACTION_ACTIVITY_RECOG = "com.example.prototype.ACTIVITY_RECOGNITION"

        // Notification
        private const val CHANNEL_ID = "sensor_service"
        private const val NOTI_ID = 1
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var nodeClient: NodeClient
    private lateinit var messageClient: MessageClient
    private var connectedNode: Node? = null

    private lateinit var activityClient: ActivityRecognitionClient
    private lateinit var pendingActivityPI: PendingIntent

    private val activityReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                Log.i(TAG, ">>> onReceive() ENTERED, action=${intent?.action}")

                val result = ActivityTransitionResult.extractResult(intent ?: return)
                if (result == null) {
                    Log.w(TAG, "No ActivityTransitionResult in intent")
                    return
                }

                for (event in result.transitionEvents) {
                    val name =
                        when (event.activityType) {
                            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
                            DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
                            DetectedActivity.ON_FOOT -> "ON_FOOT"
                            DetectedActivity.STILL -> "STILL"
                            DetectedActivity.WALKING -> "WALKING"
                            DetectedActivity.RUNNING -> "RUNNING"
                            else -> "OTHER(${event.activityType})"
                        }
                    val trans =
                        if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                            "ENTER"
                        } else {
                            "EXIT"
                        }
                    Log.i(
                        TAG,
                        "ActivityTransition: $name $trans (elapsed=${event.elapsedRealTimeNanos})",
                    )

                    if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        sendActivityUpdate(event.activityType)
                    }
                }
            }
        }

    // 센서
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var barometer: Sensor? = null
    private var stepDetector: Sensor? = null

    // 센서 데이터 버퍼
    private var timestamp = 0L
    private var ax = 0f
    private var ay = 0f
    private var az = 0f
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private var pressure = 0f
    private var stepFlag = 0f

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // 즉시 포그라운드 알림
        createNotificationChannelIfNeeded()
        val notification =
            Notification
                .Builder(this, CHANNEL_ID)
                .setContentTitle("자세 모니터링 중")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build()
        startForeground(NOTI_ID, notification)

        // 권한 체크
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "ACTIVITY_RECOGNITION permission not granted. Stopping service.")
            stopSelf()
            return
        }

        // WakeLock
        wakeLock =
            (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:WakeLock")
                .apply { acquire() }

        // Wearable 클라이언트
        nodeClient = Wearable.getNodeClient(this)
        messageClient = Wearable.getMessageClient(this)

        // ActivityTransition API 등록 (FLAG_MUTABLE 적용)
        activityClient = ActivityRecognition.getClient(this)
        val arIntent = Intent(ACTION_ACTIVITY_RECOG).setPackage(packageName)
        val piFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
        pendingActivityPI =
            PendingIntent.getBroadcast(
                this,
                0,
                arIntent,
                piFlags,
            )
        val filter = IntentFilter(ACTION_ACTIVITY_RECOG)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(activityReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(activityReceiver, filter)
        }

        // ActivityTransition API 등록
        val transitions =
            mutableListOf<ActivityTransition>().apply {
                val types =
                    intArrayOf(
                        DetectedActivity.IN_VEHICLE,
                        DetectedActivity.ON_BICYCLE,
                        DetectedActivity.ON_FOOT,
                        DetectedActivity.STILL,
                        DetectedActivity.WALKING,
                        DetectedActivity.RUNNING,
                    )
                for (t in types) {
                    add(
                        ActivityTransition
                            .Builder()
                            .setActivityType(t)
                            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                            .build(),
                    )
                    add(
                        ActivityTransition
                            .Builder()
                            .setActivityType(t)
                            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                            .build(),
                    )
                }
            }

        val transitionRequest = ActivityTransitionRequest(transitions)

        activityClient
            .requestActivityTransitionUpdates(transitionRequest, pendingActivityPI)
            .addOnSuccessListener { Log.d(TAG, "requestActivityTransitionUpdates success") }
            .addOnFailureListener { e -> Log.e(TAG, "requestActivityTransitionUpdates fail", e) }

        // 센서 등록
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        accelerometer?.let { sensorManager.registerListener(this, it, ACC_GYRO_SAMPLING_RATE) }
        gyroscope?.let { sensorManager.registerListener(this, it, ACC_GYRO_SAMPLING_RATE) }
        barometer?.let { sensorManager.registerListener(this, it, BARO_SAMPLING_RATE) }
        stepDetector?.let { sensorManager.registerListener(this, it, STEP_SAMPLING_RATE) }

        // 전송 루프 & 노드 탐색
        startPacketLoop()
        findConnectedNode()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (::sensorManager.isInitialized) {
            accelerometer?.let { sensorManager.registerListener(this, it, ACC_GYRO_SAMPLING_RATE) }
            gyroscope?.let { sensorManager.registerListener(this, it, ACC_GYRO_SAMPLING_RATE) }
            barometer?.let { sensorManager.registerListener(this, it, BARO_SAMPLING_RATE) }
            stepDetector?.let { sensorManager.registerListener(this, it, STEP_SAMPLING_RATE) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
        try {
            activityClient.removeActivityTransitionUpdates(pendingActivityPI)
        } catch (_: Exception) {
        }
        try {
            unregisterReceiver(activityReceiver)
        } catch (_: IllegalArgumentException) {
        }
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
                pressure = e.values[0]
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
            .addOnSuccessListener { stepFlag = 0f }
            .addOnFailureListener { e ->
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

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Sensor Service",
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
            }
        }
    }
}
