package com.ddc.bansoogi.common.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WatchDailyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("alarm_type")
            ?.let { AlarmType.valueOf(it) }
            ?: return

        if (!type.independentOnWatch) return

        val builder = when (type) {
            AlarmType.BREAKFAST,
            AlarmType.LUNCH,
            AlarmType.DINNER -> NotificationFactory.mealOnWatch(context, type)
            else              -> return
        }
        NotificationDispatcher.show(context, NotificationDispatcher.Id.MEAL, builder)

        val time = intent.getStringExtra("alarm_time")
        AlarmScheduler.scheduleDailyAlarm(context, type, time)
    }
}
