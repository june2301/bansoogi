package com.ddc.bansoogi.common.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ddc.bansoogi.R

class ForegroundService: Service() {

    object ServiceState {
        // 서비스 실행 상태 확인 변수
        var isRunning = false
    }


    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(1, createNotification())

        ServiceState.isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 여기에 실시간 처리 or 반복 작업 시작 로직 작성

        return START_STICKY
    }

    override fun onDestroy() {
        ServiceState.isRunning = false

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "반숙이 활동 중",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "반숙이가 활동 중 입니다."
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("반숙이 활동 중")
            .setContentText("반숙이가 활동하고 있어요")
            .setSmallIcon(R.drawable.bansoogi_default_profile)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "bansoogi_foreground_channel"
    }
}