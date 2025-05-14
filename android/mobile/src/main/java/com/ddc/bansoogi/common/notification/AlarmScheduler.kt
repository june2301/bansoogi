package com.ddc.bansoogi.common.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import java.util.*

// 알람 종류 구분용 ENUM
enum class AlarmType(
    val requestCode: Int,
    val independentOnWatch: Boolean  // true - 워치 어플 실행용
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
        time: String? // HH:MM
    ) {
        val (hour, minute) = parseTimeOrNull(time) ?: return

        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent   = Intent(context, DailyAlarmReceiver::class.java).apply {
            putExtra("alarm_type", type.name)
        }
        val pending  = PendingIntent.getBroadcast(
            context,
            type.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE,      minute)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmMgr.canScheduleExactAlarms()
        ) {
            val req = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ContextCompat.startActivity(context, req, null)

            alarmMgr.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextTime,
                AlarmManager.INTERVAL_DAY,
                pending
            )
            return
        }

        alarmMgr.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTime,
            pending
        )
    }

    /** MyInfoDto 하나를 받아 5개 알람을 모두 예약 */
    fun scheduleAllDailyAlarms(context: Context, info: MyInfoDto) {
        scheduleDailyAlarm(context, AlarmType.WAKE,      info.wakeUpTime)
        scheduleDailyAlarm(context, AlarmType.BREAKFAST, info.breakfastTime)
        scheduleDailyAlarm(context, AlarmType.LUNCH,     info.lunchTime)
        scheduleDailyAlarm(context, AlarmType.DINNER,    info.dinnerTime)
        scheduleDailyAlarm(context, AlarmType.SLEEP,     info.sleepTime)
    }

    fun cancelDailyAlarm(context: Context, type: AlarmType) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent   = Intent(context, DailyAlarmReceiver::class.java).apply {
            putExtra("alarm_type", type.name)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            type.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmMgr.cancel(pending)
        pending.cancel()
    }

    fun cancelAllDailyAlarms(context: Context) {
        AlarmType.values().forEach { cancelDailyAlarm(context, it) }
    }
}
