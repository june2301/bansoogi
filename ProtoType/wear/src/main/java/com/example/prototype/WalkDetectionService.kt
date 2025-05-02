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
            if (stepSensor == null) {
                Log.e(TAG, "STEP_DETECTOR sensor not available")
                stopSelf()
                return START_NOT_STICKY
            }
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            pressureSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }

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
            .addOnFailureListener { e ->
                Log.e(TAG, "sendActivityState failed", e)
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

    override fun onDestroy() {
        scope.cancel()
        idleJob?.cancel()
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
