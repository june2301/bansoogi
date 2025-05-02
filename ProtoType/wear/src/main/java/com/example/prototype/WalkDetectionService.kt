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
 * Foreground service that uses SensorManager Step Detector to track WALKING state
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
    private var walking = false
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
                        if (walking && System.currentTimeMillis() - lastStepTimestamp > 5000L) {
                            updateWalkState(false)
                        }
                    }
                }
        }

        return START_STICKY
    }

    private fun createNotification(): Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("걷기 감지 중")
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
        if (!walking) {
            updateWalkState(true)
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {}

    private fun updateWalkState(isWalking: Boolean) {
        if (walking == isWalking) return
        walking = isWalking
        sendWalkState(isWalking)
    }

    private fun sendWalkState(isWalking: Boolean) {
        val nodeId = connectedNode?.id ?: return
        val payload = byteArrayOf(if (isWalking) 1 else 0)
        messageClient
            .sendMessage(nodeId, WALK_STATE_PATH, payload)
            .addOnFailureListener { e ->
                Log.e(TAG, "sendWalkState failed", e)
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
