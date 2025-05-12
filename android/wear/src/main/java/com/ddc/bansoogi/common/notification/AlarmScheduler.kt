package com.ddc.bansoogi.common.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.ddc.bansoogi.myinfo.data.dto.MyInfoDto
import java.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

// 알람 종류 구분용 ENUM
enum class AlarmType(
    val requestCode: Int,
    val independentOnWatch: Boolean
) {
    WAKE(1005, false),
    SLEEP(1006, false),
    BREAKFAST(1007, true),
    LUNCH(1008, true),
    DINNER(1009, true)
}

object AlarmScheduler {

    private fun parseTimeOrNull(time: String?): Pair<Int, Int>? {
        if (time.isNullOrBlank()) return null
        val parts = time.split(":")
        if (parts.size != 2) return null
        val (h, m) = parts
        return h.toIntOrNull()?.takeIf { it in 0..23 }?.let { hh ->
            m.toIntOrNull()?.takeIf { it in 0..59 }?.let { mm ->
                hh to mm
            }
        }
    }

    fun scheduleDailyAlarm(
        context: Context,
        type: AlarmType,
        time: String?
    ) {
        Log.d("WatchSched", "scheduleDailyAlarm type=$type time=$time")
        val (hour, minute) = parseTimeOrNull(time) ?: return

        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WatchDailyAlarmReceiver::class.java).apply {
            putExtra("alarm_type", type.name)
            putExtra("alarm_time", time)
        }
        val pending  = PendingIntent.getBroadcast(
            context, type.requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE,      minute)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis())
                add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmMgr.canScheduleExactAlarms()
        ) {
            val req = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(req)
            alarmMgr.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, nextTime, AlarmManager.INTERVAL_DAY, pending
            )
        } else {
            alarmMgr.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, nextTime, pending
            )
        }
    }

    /** DataStore 에 저장된 다섯 개 시간을 한 번에 읽어와 모두 예약 */
    fun scheduleAllFromInfo(context: Context, info: MyInfoDto) {
        listOf(
            AlarmType.BREAKFAST to info.breakfastTime,
            AlarmType.LUNCH     to info.lunchTime,
            AlarmType.DINNER    to info.dinnerTime
        ).forEach { (type, t) ->
            scheduleDailyAlarm(context, type, t)
        }
    }

}