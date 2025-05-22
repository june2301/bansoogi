package com.ddc.bansoogi.common.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

object NotificationDispatcher {

    /** 알림 ID를 enum 등으로 관리하면 업데이트·취소할 때도 편리합니다. */
    enum class Id(val value: Int) {
        PHONE(1001), SITTING(1002), LYING(1003), SUMMARY(1004),
        WAKE(1005), SLEEP(1006), MEAL(1007), REWARD(1008) }

    @Volatile private var notificationEnabled = true

    fun bindNotificationEnabled(flow: Flow<Boolean>) {
        CoroutineScope(Dispatchers.Default).launch {
            flow.collect { notificationEnabled = it }
        }
    }

    fun show(context: Context, id: Id, builder: NotificationCompat.Builder) {
        if (!notificationEnabled) return

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(context).notify(id.value, builder.build())
    }

    fun cancel(context: Context, id: Id) {
        NotificationManagerCompat.from(context).cancel(id.value)
    }
}
