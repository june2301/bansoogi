package com.ddc.bansoogi.common.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DailyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 어떤 알림인지 확인
        val type = intent.getStringExtra("alarm_type")
            ?.let { AlarmType.valueOf(it) } ?: return

        // 알림 Builder 선택
        val builder = when (type) {
            AlarmType.WAKE       -> NotificationFactory.wakeUp(context)
            AlarmType.BREAKFAST  -> NotificationFactory.meal(context, type)
            AlarmType.LUNCH      -> NotificationFactory.meal(context, type)
            AlarmType.DINNER     -> NotificationFactory.meal(context, type)
            AlarmType.SLEEP      -> NotificationFactory.sleepReminder(context)
        }

        // 알림 발송
        val id = when (type) {
            AlarmType.WAKE      -> NotificationDispatcher.Id.WAKE
            AlarmType.BREAKFAST -> NotificationDispatcher.Id.MEAL
            AlarmType.LUNCH     -> NotificationDispatcher.Id.MEAL
            AlarmType.DINNER    -> NotificationDispatcher.Id.MEAL
            AlarmType.SLEEP     -> NotificationDispatcher.Id.SLEEP
        }
        NotificationDispatcher.show(context, id, builder)

        // 다음 날 같은 알람 재예약
        CoroutineScope(Dispatchers.IO).launch {
            val info = MyInfoModel().getMyInfo().first()
            withContext(Dispatchers.Main) {
                when (type) {
                    AlarmType.WAKE ->
                        AlarmScheduler.scheduleDailyAlarm(context, type, info.wakeUpTime)

                    AlarmType.BREAKFAST ->
                        AlarmScheduler.scheduleDailyAlarm(context, type, info.breakfastTime)

                    AlarmType.LUNCH ->
                        AlarmScheduler.scheduleDailyAlarm(context, type, info.lunchTime)

                    AlarmType.DINNER ->
                        AlarmScheduler.scheduleDailyAlarm(context, type, info.dinnerTime)

                    AlarmType.SLEEP ->
                        AlarmScheduler.scheduleDailyAlarm(context, type, info.sleepTime)
                }
            }
        }
    }
}
