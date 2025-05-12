package com.ddc.bansoogi.common.notification

import android.util.Log
import com.ddc.bansoogi.common.mobile.communication.CommunicationPaths
import com.ddc.bansoogi.common.mobile.data.mapper.JsonMapper
import com.ddc.bansoogi.myinfo.data.dto.MyInfoDto
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyInfoListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == CommunicationPaths.MobileToWear.MY_INFO_DATA) {
            try {
                val info = JsonMapper.fromJson<MyInfoDto>(String(event.data))
                // 모바일에서 받은 MyInfoDto 로 바로 알림 예약
                GlobalScope.launch(Dispatchers.IO) {
                    AlarmScheduler.scheduleDailyAlarm(
                        applicationContext, AlarmType.WAKE, info.wakeUpTime
                    )
                    AlarmScheduler.scheduleDailyAlarm(
                        applicationContext, AlarmType.BREAKFAST, info.breakfastTime
                    )
                    AlarmScheduler.scheduleDailyAlarm(
                        applicationContext, AlarmType.LUNCH, info.lunchTime
                    )
                    AlarmScheduler.scheduleDailyAlarm(
                        applicationContext, AlarmType.DINNER, info.dinnerTime
                    )
                    AlarmScheduler.scheduleDailyAlarm(
                        applicationContext, AlarmType.SLEEP, info.sleepTime
                    )
                }
            } catch (e: Exception) {
                Log.e("MyInfoMsgRecv", "failed to parse MyInfoDto", e)
            }
        }
    }
}
