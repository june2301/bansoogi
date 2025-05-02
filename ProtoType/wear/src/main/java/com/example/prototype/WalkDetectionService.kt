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
    private var idleJob: Job? = null
    private lateinit var nodeClient: NodeClient
    private lateinit var messageClient: MessageClient
    private var connectedNode: Node? = null
    private var started = false

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
            if (stepSensor == null) {
                Log.e(TAG, "STEP_DETECTOR sensor not available")
                stopSelf()
                return START_NOT_STICKY
            }
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)

            // launch idle checker
            idleJob =
                scope.launch {
                    while (isActive) {
                        delay(1000)
                        val now = System.currentTimeMillis()
                        while (stepTimestamps.isNotEmpty() && now - stepTimestamps.first() > 5000L) {
                            stepTimestamps.removeFirst()
                        }
                        val cadenceSpm = if (stepTimestamps.isNotEmpty()) stepTimestamps.size * 12 else 0
                        val newState =
                            when {
                                stepTimestamps.isEmpty() || now - lastStepTimestamp > 5000L -> 0
                                cadenceSpm >= 150 -> 2
                                else -> 1
                            }
                        Log.d(TAG, "SPM=$cadenceSpm, newState=$newState")
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
        if (event.sensor.type != Sensor.TYPE_STEP_DETECTOR) return
        lastStepTimestamp = System.currentTimeMillis()
        stepTimestamps.addLast(lastStepTimestamp)
        while (stepTimestamps.isNotEmpty() && lastStepTimestamp - stepTimestamps.first() > 5000L) {
            stepTimestamps.removeFirst()
        }
        updateActivityState(1)
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
