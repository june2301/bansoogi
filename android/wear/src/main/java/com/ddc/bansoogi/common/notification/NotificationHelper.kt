package com.ddc.bansoogi.common.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_WAKE  = "channel_wake"
    const val CHANNEL_MEAL  = "channel_meal"
    const val CHANNEL_SLEEP = "channel_sleep"

    fun registerChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(CHANNEL_WAKE,  "기상 알림", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_MEAL,  "식사 알림", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_SLEEP, "취침 알림", NotificationManager.IMPORTANCE_HIGH),
        ).onEach { ch ->
            ch.description = when (ch.id) {
                CHANNEL_WAKE  -> "설정한 기상 시간에 알림 발송"
                CHANNEL_MEAL  -> "설정한 식사 시간에 알림 발송"
                CHANNEL_SLEEP -> "설정한 취침 시간에 알림 발송"
                else          -> ""
            }
        }
        manager.createNotificationChannels(channels)
    }
}
