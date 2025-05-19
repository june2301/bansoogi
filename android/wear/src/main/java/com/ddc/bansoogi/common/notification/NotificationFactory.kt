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
            AlarmType.BREAKFAST -> "아침 식사 시간이에요"
            AlarmType.LUNCH     -> "점심 식사 시간이에요"
            AlarmType.DINNER    -> "저녁 식사 시간이에요"
            else                -> "식사 시간이에요"
        }
        val pi = buildWatchDeepLinkPendingIntent(
            context,
            "bansoogi://watch/meal?type=${type.name.lowercase()}",
            type.requestCode
        )
        return baseBuilder(context, NotificationHelper.CHANNEL_MEAL)
            .setContentTitle("식사 알림")
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
            .setContentTitle("기상 알림")
            .setContentText("상쾌한 하루를 시작해 볼까요?")
            .setContentIntent(pi)
    }

    fun sleepReminderOnWatch(context: Context): NotificationCompat.Builder {
        val pi = buildWatchDeepLinkPendingIntent(
            context,
            "bansoogi://watch/sleep",
            AlarmType.SLEEP.requestCode
        )
        return baseBuilder(context, NotificationHelper.CHANNEL_SLEEP)
            .setContentTitle("취침 알림")
            .setContentText("하루를 마무리하고 푹 쉬세요!")
            .setContentIntent(pi)
    }
}