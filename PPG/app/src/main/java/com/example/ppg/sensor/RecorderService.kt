class RecorderService : Service() {
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
        if (!::trackingService.isInitialized) {
            initSamsungSdk()
        }
        if (ppgTracker == null) {
            connectionListener.onConnectionSuccess()
        }
    }

    override fun onDestroy() {
        ppgTracker?.unsetEventListener()
        trackingService.disconnectService()
        stopForeground(true)
        super.onDestroy()
    }
}