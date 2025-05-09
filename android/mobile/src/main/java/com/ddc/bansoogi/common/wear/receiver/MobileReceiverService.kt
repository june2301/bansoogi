package com.ddc.bansoogi.common.wear.receiver

import android.util.Log
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.wear.data.mapper.WearDtoMapper
import com.ddc.bansoogi.common.wear.sender.MyInfoSender
import com.ddc.bansoogi.common.wear.sender.TodayRecordSender
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MobileReceiverService: WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            "/today_record_request" -> handleTodayRecordRequest()
            "/my_info_request" -> handleMyInfoRequest()

            else -> Log.w("WatchReceiver", "알 수 없는 경로: ${messageEvent.path}")
        }
    }

    private fun handleTodayRecordRequest() {
        CoroutineScope(Dispatchers.IO).launch {
            val model = TodayRecordModel()
            val dto = model.getTodayRecordOnce()

            dto?.let {
                val wearDto = WearDtoMapper.toReport(it)

                TodayRecordSender.send(applicationContext, wearDto)
            }
        }
    }

    private fun handleMyInfoRequest() {
        CoroutineScope(Dispatchers.IO).launch {
            val model = MyInfoModel()
            val dto = model.getMyInfoOnce()

            dto?.let {
                val wearDto = WearDtoMapper.toMyInfo(it)

                MyInfoSender.send(applicationContext, wearDto)
            }
        }
    }
}