package com.ddc.bansoogi.common.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import com.ddc.bansoogi.R

object NotificationFactory {

    fun phoneUsage(context: Context, minutes: Int): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_PHONE_USAGE)
            .setContentTitle("휴대폰 사용 과다")
            .setContentText("$minutes 분 넘게 사용 중입니다. 잠시 휴식하세요!")

    fun sitting(context: Context, minutes: Int): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_SITTING)
            .setContentTitle("장시간 앉아 있음")
            .setContentText("$minutes 분 동안 앉아 있었습니다. 잠시 스트레칭 어떠세요?")

    fun lying(context: Context, minutes: Int): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_LYING)
            .setContentTitle("장시간 누워 있음")
            .setContentText("$minutes 분 동안 누워 있었습니다. 몸을 움직여 보세요.")

    fun dailySummary(context: Context): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_TODAY_RECORD)
            .setContentTitle("오늘의 활동 리포트")
            .setContentText("터치해서 상세 내용을 확인하세요.")

    fun sleepReminder(context: Context): NotificationCompat.Builder =
        baseBuilder(context, NotificationHelper.CHANNEL_SLEEP)
            .setContentTitle("취침 시간이 되었어요")
            .setContentText("하루를 마무리하고 푹 쉬세요!")

    fun wakeUp(context: Context) =
        baseBuilder(context, NotificationHelper.CHANNEL_WAKE)
            .setContentTitle("기상 시간이에요")
            .setContentText("상쾌한 하루를 시작해 볼까요?")

    fun meal(context: Context, content: String) =
        baseBuilder(context, NotificationHelper.CHANNEL_MEAL)
            .setContentTitle("식사 알림")
            .setContentText(content)

    /* 공통 속성을 모아 둔 private 메서드 */
    private fun baseBuilder(context: Context, channelId: String) =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_home)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
}
