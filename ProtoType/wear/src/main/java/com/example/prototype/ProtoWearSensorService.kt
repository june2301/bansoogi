package com.example.prototype

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
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

class ProtoWearSensorService :
    Service(),
    SensorEventListener {
    companion object {
        private const val TAG = "ProtoWearSensorService"

        // 센서 샘플링 속도
        private const val ACC_GYRO_SAMPLING_RATE = SensorManager.SENSOR_DELAY_GAME // ~25 Hz
        private const val BARO_SAMPLING_RATE = SensorManager.SENSOR_DELAY_UI // ~10 Hz

        // 패킷 크기 및 전송 간격
        private const val PACKET_INTERVAL_MS = 250L

        // DataLayer paths
        private const val SENSOR_DATA_PATH = "/sensor_data"
    }

    // System Services
    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock

    // Wearable API
    private lateinit var nodeClient: com.google.android.gms.wearable.NodeClient
    private lateinit var messageClient: MessageClient
    private var connectedNode: Node? = null

    // 센서 객체
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var barometer: Sensor? = null

    // 센서 데이터 버퍼
    private var timestamp: Long = 0
    private var ax: Float = 0f
    private var ay: Float = 0f
    private var az: Float = 0f
    private var gx: Float = 0f
    private var gy: Float = 0f
    private var gz: Float = 0f
    private var pressure: Float = 0f

    // 코루틴 스코프
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // 웨이크락 획득
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager
                .newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "ProtoType:WakeLock",
                ).apply { acquire() }

        // Wearable API 클라이언트 초기화
        nodeClient = Wearable.getNodeClient(this)
        messageClient = Wearable.getMessageClient(this)

        // 센서 매니저 초기화
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // 센서 객체 가져오기
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        // 태스크 시작
        setupDataTransferTask()
        findConnectedNode()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // 센서 리스너 등록
        accelerometer?.let {
            sensorManager.registerListener(this, it, ACC_GYRO_SAMPLING_RATE)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, ACC_GYRO_SAMPLING_RATE)
        }
        barometer?.let {
            sensorManager.registerListener(this, it, BARO_SAMPLING_RATE)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        // 센서 리스너 해제
        sensorManager.unregisterListener(this)

        // 코루틴 스코프 취소
        serviceScope.cancel()

        // 웨이크락 해제
        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                ax = event.values[0]
                ay = event.values[1]
                az = event.values[2]
                timestamp = event.timestamp
            }
            Sensor.TYPE_GYROSCOPE -> {
                gx = event.values[0]
                gy = event.values[1]
                gz = event.values[2]
            }
            Sensor.TYPE_PRESSURE -> {
                pressure = event.values[0] // hPa
            }
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {
        // 정확도 변경 처리
    }

    private fun setupDataTransferTask() {
        serviceScope.launch {
            while (isActive) {
                sendSensorData()
                delay(PACKET_INTERVAL_MS)
            }
        }
    }

    private fun findConnectedNode() {
        serviceScope.launch {
            try {
                // 연결된 노드 찾기
                val nodes =
                    Tasks.await(
                        nodeClient.connectedNodes,
                        5,
                        TimeUnit.SECONDS,
                    )

                // 첫 번째 연결된 노드 가져오기
                connectedNode = nodes.firstOrNull()
                Log.d(TAG, "Connected node: ${connectedNode?.displayName ?: "None"}")
            } catch (e: Exception) {
                Log.e(TAG, "Error finding connected node", e)
            }
        }
    }

    private fun sendSensorData() {
        val nodeId = connectedNode?.id ?: return

        // ByteBuffer로 센서 데이터 직렬화
        val buffer = ByteBuffer.allocate(8 * 8) // 8 doubles (t, ax, ay, az, gx, gy, gz, p)

        // 센서 데이터 추가
        buffer.putDouble(timestamp.toDouble())
        buffer.putDouble(ax.toDouble())
        buffer.putDouble(ay.toDouble())
        buffer.putDouble(az.toDouble())
        buffer.putDouble(gx.toDouble())
        buffer.putDouble(gy.toDouble())
        buffer.putDouble(gz.toDouble())
        buffer.putDouble(pressure.toDouble())

        // 메시지 전송
        messageClient
            .sendMessage(nodeId, SENSOR_DATA_PATH, buffer.array())
            .addOnSuccessListener {
                Log.v(TAG, "Data sent successfully")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error sending data", e)
                // 노드 연결이 끊어진 경우 재연결 시도
                findConnectedNode()
            }
    }
}
