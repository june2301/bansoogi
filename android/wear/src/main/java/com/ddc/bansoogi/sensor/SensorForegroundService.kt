package com.ddc.bansoogi.sensor

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ddc.bansoogi.activity.ActivityStateProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Foreground service that keeps [AndroidSensorManager] alive while the app is not in the foreground,
 * 그리고 ActivityStateProcessor를 실행하여 분류 결과를 로그로 찍습니다.
 */
class SensorForegroundService : Service() {

    private val sensorManager by lazy { AndroidSensorManager(this) }

    // 분류기와 서비스 전용 CoroutineScope
    private lateinit var activityProcessor: ActivityStateProcessor
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // 1) Notification 띄워서 포그라운드로 전환
        startForeground(NOTIFICATION_ID, createNotification())

        // 2) 센서 구독 시작
        sensorManager.startAll()

        // 3) ActivityStateProcessor 초기화 및 실행
        activityProcessor = ActivityStateProcessor(sensorManager, externalScope = serviceScope).also {
            it.start()
        }

        // 4) 분류 결과를 로그로 출력
        activityProcessor.state
            .onEach { state ->
                Log.d(TAG, "★ ActivityState → $state")
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY so that the system restarts the service if it is killed.
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Schedule quick restart in case the task is swiped away.
        val restartIntent = Intent(applicationContext, SensorForegroundService::class.java)
        val restartPendingIntent = PendingIntent.getService(
            this,
            1,
            restartIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + RESTART_DELAY_MS,
            restartPendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // 1) 센서 정리
        sensorManager.stopAll()
        // 2) 분류기 정지
        activityProcessor.stop()
        // 3) 스코프 취소
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = NOTIFICATION_CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sensor foreground service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bansoogi is measuring")
            .setContentText("Collecting sensor data…")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "sensor_foreground"
        private const val RESTART_DELAY_MS = 1_000L
        private const val TAG = "SensorForegroundSvc"

        fun ensureRunning(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, SensorForegroundService::class.java))
        }
    }
}
