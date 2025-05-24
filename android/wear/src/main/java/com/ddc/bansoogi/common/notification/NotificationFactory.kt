package com.ddc.bansoogi.common.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import com.ddc.bansoogi.R

object NotificationFactory {
    private fun baseBuilder(context: Context, channelId: String) =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_watch_home)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

    fun mealOnWatch(context: Context, type: AlarmType): NotificationCompat.Builder {
        val text = when (type) {
            AlarmType.BREAKFAST -> "아침 시간~"
            AlarmType.LUNCH     -> "점심 먹고 가자~"
            AlarmType.DINNER    -> "맛있는거 먹자!"
            else                -> "밥 먹어! 밥!"
        }
        val pi = buildWatchDeepLinkPendingIntent(
            context,
            "bansoogi://watch/meal?type=${type.name.lowercase()}",
            type.requestCode
        )
        return baseBuilder(context, NotificationHelper.CHANNEL_MEAL)
            .setContentTitle("밥 먹자!")
            .setContentText(text)
            .setContentIntent(pi)
    }

    fun wakeUpOnWatch(context: Context): NotificationCompat.Builder {
        val pi = buildWatchDeepLinkPendingIntent(
            context,
            "bansoogi://watch/wake",
            AlarmType.WAKE.requestCode
        )
        return baseBuilder(context, NotificationHelper.CHANNEL_WAKE)
            .setContentTitle("일어날 시간이야!")
            .setContentText("오늘 하루도 화이팅!")
            .setContentIntent(pi)
    }

    fun sleepReminderOnWatch(context: Context): NotificationCompat.Builder {
        val pi = buildWatchDeepLinkPendingIntent(
            context,
            "bansoogi://watch/sleep",
            AlarmType.SLEEP.requestCode
        )
        return baseBuilder(context, NotificationHelper.CHANNEL_SLEEP)
            .setContentTitle("잘 시간이야")
            .setContentText("수고했어! 오늘도!")
            .setContentIntent(pi)
    }
}