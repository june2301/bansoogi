package com.ddc.bansoogi.common.notification

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationDispatcher {
    enum class Id(val value: Int) {
        WAKE(1005), MEAL(1007), SLEEP(1006)
    }

    @Volatile private var notificationEnabled = true

    @SuppressLint("MissingPermission")
    fun show(context: Context, id: Id, builder: NotificationCompat.Builder) {
        if (!notificationEnabled) return
        // Wear OS 에선 런타임 알림 권한 체크 불필요
        NotificationManagerCompat.from(context).notify(id.value, builder.build())
    }

    fun cancel(context: Context, id: Id) {
        NotificationManagerCompat.from(context).cancel(id.value)
    }
}
