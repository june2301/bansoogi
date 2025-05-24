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
                GlobalScope.launch(Dispatchers.IO) {
                    AlarmScheduler.rescheduleMealAlarms(applicationContext, info)
                }
            } catch (e: Exception) {
                Log.e("MyInfoMsgRecv", "failed to parse MyInfoDto", e)
            }
        }
    }
}
