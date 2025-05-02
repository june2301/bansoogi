package com.example.prototype

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.health.services.client.data.UserActivityInfo
import androidx.health.services.client.data.UserActivityState
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Foreground service that uses Health Services PassiveMonitoring to track WALKING state
 * and synchronise it to the paired phone via the Data Layer.
 */
class WalkDetectionService : Service() {
    companion object {
        private const val TAG = "WalkDetectionSvc"
        private const val CHANNEL_ID = "walk_detection"
        private const val NOTI_ID = 2
        private const val WALK_STATE_PATH = "/walk_state" // Data Layer path
    }

    private lateinit var passiveClient: PassiveMonitoringClient
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

            if (!checkPerms()) {
                Log.w(TAG, "ACTIVITY_RECOGNITION permission not granted. stopping")
                stopSelf()
                return START_NOT_STICKY
            }

            nodeClient = Wearable.getNodeClient(this)
            messageClient = Wearable.getMessageClient(this)
            findNode()

            passiveClient = HealthServices.getClient(this).passiveMonitoringClient
            registerPassiveListener()
        }

        return START_STICKY
    }

    private fun checkPerms(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

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

    private fun registerPassiveListener() {
        val cfg =
            PassiveListenerConfig
                .Builder()
                .setShouldUserActivityInfoBeRequested(true)
                .build()

        passiveClient.setPassiveListenerCallback(
            cfg,
            object : PassiveListenerCallback {
                override fun onUserActivityInfoReceived(info: UserActivityInfo) {
                    val walking = info.userActivityState == UserActivityState.USER_ACTIVITY_EXERCISE
                    sendWalkState(walking)
                }

                override fun onNewDataPointsReceived(data: androidx.health.services.client.data.DataPointContainer) {
                    // not used
                }

                override fun onGoalCompleted(goal: androidx.health.services.client.data.PassiveGoal) {}

                override fun onRegistrationFailed(throwable: Throwable) {
                    Log.e(TAG, "Passive listener registration failed", throwable)
                }
            },
        )
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
        passiveClient.clearPassiveListenerCallbackAsync()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
