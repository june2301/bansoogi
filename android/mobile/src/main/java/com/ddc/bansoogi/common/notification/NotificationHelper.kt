package com.ddc.bansoogi.common.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

// 앱에서 사용하는 모든 알림 채널을 한곳에서 관리합니다.
object NotificationHelper {

    // 채널 ID는 외부(서비스·Worker 등)에서 사용하므로 public const 로 노출
    const val CHANNEL_PHONE_USAGE  = "channel_phone_usage"
    const val CHANNEL_SITTING      = "channel_sitting"
    const val CHANNEL_LYING        = "channel_lying"
    const val CHANNEL_TODAY_RECORD = "channel_today_record"

    const val CHANNEL_WAKE       = "channel_wake"
    const val CHANNEL_MEAL       = "channel_meal"
    const val CHANNEL_SLEEP      = "channel_sleep"
    const val CHANNEL_REWARD        =    "channel_reward"   // ✨ 추가

    // Application.onCreate() 에서 딱 한 번 호출
    fun registerChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                CHANNEL_PHONE_USAGE,
                "휴대폰 과다 사용 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "휴대폰 사용 시간을 초과했을 경우 알림 발송" },

            NotificationChannel(
                CHANNEL_SITTING,
                "장시간 앉아 있음 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "설정한 시간보다 오랜 시간 앉아 있을 경우 알림 발송" },

            NotificationChannel(
                CHANNEL_LYING,
                "장시간 누워 있음 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "설정한 시간보다 오랜 시간 누워 있을 경우 알림 발송" },

            NotificationChannel(
                CHANNEL_TODAY_RECORD,
                "하루 생활 결산",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "하루 생활 패턴 결산 알림 발송" },

            NotificationChannel(
                CHANNEL_WAKE,
                "기상 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "설정한 기상 시간에 알림 발송" },

            NotificationChannel(
                CHANNEL_MEAL,
                "식사 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "설정한 식사 시간에 알림 발송" },

            NotificationChannel(
                CHANNEL_SLEEP,
                "취침 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "설정한 취침 시간에 알림 발송" },

            NotificationChannel(
                CHANNEL_REWARD,
                "즉시 행동 보상 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "일어서기/스트레칭 성공 시 에기가 기뻐합니다!" }
        )
        manager.createNotificationChannels(channels)
    }
}
