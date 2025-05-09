package com.ddc.bansoogi.common.wear.receiver

import android.util.Log
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.wear.data.mapper.WearDtoMapper
import com.ddc.bansoogi.common.wear.sender.TodayRecordSender
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayRecordRequestReceiverService: WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WatchReceiver", "onMessageReceived 호출됨: ${messageEvent.path}")
        super.onMessageReceived(messageEvent)

        if (messageEvent.path == "/today_record_request") {
            CoroutineScope(Dispatchers.IO).launch {
                val model = TodayRecordModel()
                val dto = model.getTodayRecordOnce()

                dto?.let {
                    val wearDto = WearDtoMapper.toReport(it)

                    TodayRecordSender.send(applicationContext, wearDto)
                }
            }
        }
    }
}