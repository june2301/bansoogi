package com.example.prototype

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.prototype.pose.DynamicSkip
import com.example.prototype.pose.TFLiteStaticPoseClassifier
import com.example.prototype.pose.WindowBuffer
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Foreground service that uses SensorManager Step Detector to track activity state
 * and synchronise it to the paired phone via the Data Layer.
 */
class WalkDetectionService :
    Service(),
    SensorEventListener {
    companion object {
        private const val TAG = "WalkDetectionSvc"
        private const val CHANNEL_ID = "walk_detection"
        private const val NOTI_ID = 2
        private const val WALK_STATE_PATH = "/walk_state" // Data Layer path
        private const val POSE_STATE_PATH = "/pose_state" // Data Layer path
    }

    private lateinit var sensorManager: SensorManager
    private var lastStepTimestamp: Long = 0L
    private var activityState: Int = 0
    private val stepTimestamps = ArrayDeque<Long>()
    private val altitudeWindow = ArrayDeque<Pair<Long, Float>>()
    private var stairCandidate = false // altitude threshold met
    private var altitudeLPF = 0f
    private var idleJob: Job? = null
    private lateinit var nodeClient: NodeClient
    private lateinit var messageClient: MessageClient
    private var connectedNode: Node? = null
    private var started = false
    private lateinit var poseClassifier: TFLiteStaticPoseClassifier
    private lateinit var accWindowBuffer: WindowBuffer
    private lateinit var linWindowBuffer: WindowBuffer
    private var poseHysteresis: IntArray = intArrayOf(-1, -1) // last two pose outputs
    private var queuedRawWindow: FloatArray? = null
    private val gravity = FloatArray(3)
    private val ALPHA = 0.9f

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // --- tuning constants ---
    private val ALT_TH_METERS = 0.5f // 0.5 m ascent threshold
    private val STAIR_WINDOW_MS = 6000L // 6-second window
    private val MIN_STEPS_BASE = 3 // minimum steps even for slow ascent
    private val LPF_ALPHA = 0.25f // low-pass filter factor for altitude

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (!started) {
            started = true
            startForeground(NOTI_ID, createNotification())

            nodeClient = Wearable.getNodeClient(this)
            messageClient = Wearable.getMessageClient(this)
            findNode()

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
            val accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (stepSensor == null) {
                Log.e(TAG, "STEP_DETECTOR sensor not available")
                stopSelf()
                return START_NOT_STICKY
            }
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            pressureSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME, 25000) // raw for CNN

            poseClassifier = TFLiteStaticPoseClassifier(this)
            accWindowBuffer = WindowBuffer(125)
            linWindowBuffer = WindowBuffer(125)

            // launch idle checker
            idleJob =
                scope.launch {
                    while (isActive) {
                        delay(1000)
                        val now = System.currentTimeMillis()
                        while (stepTimestamps.isNotEmpty() && now - stepTimestamps.first() > 5000L) {
                            stepTimestamps.removeFirst()
                        }
                        val stepsWin = stepTimestamps.count { now - it <= STAIR_WINDOW_MS }
                        val cadenceSpm = if (stepsWin > 0) stepsWin * 60000 / STAIR_WINDOW_MS.toInt() else 0

                        // 가변 최소 스텝: 윈도우 내 스텝의 40% 또는 MIN_STEPS_BASE 중 큰 값
                        val minSteps = maxOf(MIN_STEPS_BASE, (stepsWin * 0.4f).toInt())

                        val ascending = stairCandidate && stepsWin >= minSteps
                        val baseState =
                            when {
                                stepTimestamps.isEmpty() || now - lastStepTimestamp > 5000L -> 0
                                cadenceSpm >= 150 -> 2
                                else -> 1
                            }

                        val newState = if (ascending) 3 else baseState
                        Log.d(TAG, "SPM=$cadenceSpm, ascending=$ascending, newState=$newState")
                        updateActivityState(newState)
                    }
                }
        }

        return START_STICKY
    }

    private fun createNotification(): Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("활동 감지 중")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Walk Detection", NotificationManager.IMPORTANCE_LOW),
                )
            }
        }
    }

    // Sensor callbacks
    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                lastStepTimestamp = now
                stepTimestamps.addLast(now)
                while (stepTimestamps.isNotEmpty() && now - stepTimestamps.first() > 5000L) {
                    stepTimestamps.removeFirst()
                }
                // immediate provisional update to walking if idle
                if (activityState == 0) updateActivityState(1)
            }
            Sensor.TYPE_PRESSURE -> {
                val pressure = event.values[0]
                val altitude =
                    android.hardware.SensorManager.getAltitude(
                        android.hardware.SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                        pressure,
                    )
                if (altitudeLPF == 0f) altitudeLPF = altitude // init
                altitudeLPF += LPF_ALPHA * (altitude - altitudeLPF)

                altitudeWindow.addLast(now to altitudeLPF)
                while (altitudeWindow.isNotEmpty() && now - altitudeWindow.first().first > STAIR_WINDOW_MS) {
                    altitudeWindow.removeFirst()
                }
                if (altitudeWindow.size >= 2) {
                    val deltaH = altitudeLPF - altitudeWindow.first().second
                    stairCandidate = deltaH > ALT_TH_METERS
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                // low-pass to estimate gravity
                gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * ax
                gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * ay
                gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * az

                val linX = ax - gravity[0]
                val linY = ay - gravity[1]
                val linZ = az - gravity[2]

                val rawWin = accWindowBuffer.addSample(ax, ay, az)
                val linWin = linWindowBuffer.addSample(linX, linY, linZ)

                rawWin?.let { queuedRawWindow = it }

                linWin?.let { linWindow ->
                    val skip = DynamicSkip.shouldSkipLinear(linWindow)
                    Log.d(TAG, "LIN SMA=${"%.2f".format(computeSma(linWindow))} skip=$skip")
                    if (skip) return@let
                    queuedRawWindow?.let { raw ->
                        val pose = poseClassifier.predict(raw)
                        poseHysteresis[0] = poseHysteresis[1]
                        poseHysteresis[1] = pose
                        if (poseHysteresis[0] == poseHysteresis[1]) updatePoseState(pose)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {}

    private fun updateActivityState(state: Int) {
        if (activityState == state) return
        activityState = state
        sendActivityState(state)
    }

    private fun sendActivityState(state: Int) {
        val nodeId = connectedNode?.id ?: return
        val payload = byteArrayOf(state.toByte())
        messageClient
            .sendMessage(nodeId, WALK_STATE_PATH, payload)
            .addOnSuccessListener { Log.d(TAG, "sent WALK_STATE=$state") }
            .addOnFailureListener { e ->
                Log.e(TAG, "sendActivityState failed", e)
                findNode()
            }
    }

    private fun updatePoseState(pose: Int) {
        // Pose codes: 0 sitting,1 lying,2 standing. We map to 10+pose for activityState
        val mappedState = 10 + pose
        if (activityState == mappedState) return
        activityState = mappedState
        sendPoseState(pose)
    }

    private fun sendPoseState(pose: Int) {
        val nodeId = connectedNode?.id ?: return
        val payload = byteArrayOf(pose.toByte())
        messageClient
            .sendMessage(nodeId, POSE_STATE_PATH, payload)
            .addOnSuccessListener { Log.d(TAG, "sent POSE_STATE=$pose") }
            .addOnFailureListener { e ->
                Log.e(TAG, "sendPoseState failed", e)
                findNode()
            }
    }

    private fun findNode() =
        scope.launch {
            try {
                connectedNode = Tasks.await(nodeClient.connectedNodes, 5, TimeUnit.SECONDS).firstOrNull()
                Log.d(TAG, "connected node ${connectedNode?.displayName}")
            } catch (e: Exception) {
                Log.e(TAG, "findNode error", e)
            }
        }

    private fun computeSma(window: FloatArray): Float {
        val n = window.size / 3
        var sum = 0f
        for (i in 0 until n) {
            sum += abs(window[3 * i]) + abs(window[3 * i + 1]) + abs(window[3 * i + 2])
        }
        return sum / n
    }

    override fun onDestroy() {
        scope.cancel()
        idleJob?.cancel()
        sensorManager.unregisterListener(this)
        poseClassifier.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
