import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Add stub extension functions to satisfy compiler until real SDK is integrated
private fun Any.setEventListener(listener: Any) {
    // TODO: Replace with actual SDK call when available
}

private fun Any.unsetEventListener() {
    // TODO: Replace with actual SDK call when available
}

class RecorderService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var ppgTracker: Any? = null
    private val ppgListener: Any = Any()
    private val trackingService = object {
        fun disconnectService() {}
    }
    private val connectionListener = object {
        fun onConnectionSuccess() {}
    }

    companion object {
        private const val CHANNEL_ID = "RecorderChannel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_INIT = "com.example.ppg.action.INIT"
        const val ACTION_START = "com.example.ppg.action.START"
        const val ACTION_STOP = "com.example.ppg.action.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INIT -> {
                serviceScope.launch { initSamsungSdk() }
            }
            ACTION_START -> {
                serviceScope.launch {
                    ensureTrackerReady()
                    ppgTracker?.setEventListener(ppgListener)
                    startForeground(NOTIFICATION_ID, buildNotification("Recording…"))
                }
            }
            ACTION_STOP -> {
                serviceScope.launch {
                    ppgTracker?.unsetEventListener()
                    stopForeground(true)
                }
            }
        }
        return START_STICKY
    }

    private suspend fun ensureTrackerReady() {
        initSamsungSdk()
        if (ppgTracker == null) {
            connectionListener.onConnectionSuccess()
        }
    }

    private suspend fun initSamsungSdk() {
        // TODO: Initialize Samsung SDK or tracker here
    }

    override fun onDestroy() {
        serviceScope.cancel()
        ppgTracker?.unsetEventListener()
        trackingService.disconnectService()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recorder notifications",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("PPG Recorder")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("PPG Recorder")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        }
    }
}