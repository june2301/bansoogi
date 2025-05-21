package com.ddc.bansoogi.common.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import com.ddc.bansoogi.R

object NotificationFactory {

    fun phoneUsage(context: Context, minutes: Int): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_PHONE_USAGE)
            .setContentTitle("핸드폰 그만해!")
            .setContentText("$minutes 분이나 핸드폰?")
            .setContentIntent(
                buildDeepLinkPendingIntent(
                    context,
                    "bansoogi://home",
                    NotificationDispatcher.Id.PHONE.value
                )
            )

    fun sitting(context: Context, minutes: Int): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_SITTING)
            .setContentTitle("좀 쉬었다 해!")
            .setContentText("$minutes 분 동안 앉아 있었어!")
            .setContentIntent(
                buildDeepLinkPendingIntent(
                    context,
                    "bansoogi://home",
                    NotificationDispatcher.Id.SITTING.value
                )
            )

    fun lying(context: Context, minutes: Int): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_LYING)
            .setContentTitle("일어나! 이 게으름뱅이야!")
            .setContentText("$minutes 분 동안 누워 있었어!")
            .setContentIntent(
                buildDeepLinkPendingIntent(
                    context,
                    "bansoogi://home",
                    NotificationDispatcher.Id.LYING.value
                )
            )

    fun dailySummary(context: Context): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_TODAY_RECORD)
            .setContentTitle("활동 리포트 도착!")
            .setContentText("오늘 활동 내용을 확인해봐!")
            .setContentIntent(
                buildDeepLinkPendingIntent(
                    context,
                    "bansoogi://home",
                    NotificationDispatcher.Id.SUMMARY.value
                )
            )

    fun sleepReminder(context: Context): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_SLEEP)
            .setContentTitle("잘 시간이야")
            .setContentText("수고했어! 오늘도!")
            .setContentIntent(
                buildDeepLinkPendingIntent(
                    context,
                    "bansoogi://home",
                    NotificationDispatcher.Id.SLEEP.value
                )
            )

    fun wakeUp(context: Context) =
        baseBuilder(context, NotificationHelper.CHANNEL_WAKE)
            .setContentTitle("일어날 시간이야!")
            .setContentText("오늘 하루도 화이팅!")
            .setContentIntent(
                buildDeepLinkPendingIntent(
                    context,
                    "bansoogi://home",
                    NotificationDispatcher.Id.WAKE.value
                )
            )

    fun meal(
        context: Context,
        type: AlarmType
    ): NotificationCompat.Builder {
        val description = when (type) {
            AlarmType.BREAKFAST -> "아침 시간~"
            AlarmType.LUNCH     -> "점심 먹고 가자~"
            AlarmType.DINNER    -> "맛있는거 먹자!"
            else                -> "밥 먹어! 밥!"
        }

        val builder = baseBuilder(context, NotificationHelper.CHANNEL_MEAL)
            .setContentTitle("밥 먹자!")
            .setContentText(description)
            .setContentIntent(
                buildDeepLinkPendingIntent(
                    context,
                    "bansoogi://home",
                    NotificationDispatcher.Id.WAKE.value
                )
            )

        // 폰／워치 독립 여부 처리
        if (type.independentOnWatch) {
            builder.setLocalOnly(true)
        }

        return builder
    }

    /** 즉시 행동 보상(일어서기·스트레칭) */
    fun cheer(context: Context): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_REWARD)
           .setContentTitle("포인트 획득!")
           .setContentText("잘 했어~!")
           .setContentIntent(
                buildDeepLinkPendingIntent(
                    context,
                    "bansoogi://home",
                    NotificationDispatcher.Id.REWARD.value
                )
            )

    /* 공통 속성을 모아 둔 private 메서드 */
    private fun baseBuilder(context: Context, channelId: String) =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_home)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
}
