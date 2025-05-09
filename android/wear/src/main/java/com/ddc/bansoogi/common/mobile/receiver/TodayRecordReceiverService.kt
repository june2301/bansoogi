package com.ddc.bansoogi.common.mobile.receiver

import android.content.Context
import android.util.Log
import com.ddc.bansoogi.today.data.dto.ReportDto
import com.ddc.bansoogi.today.data.store.saveReportCache
import com.ddc.bansoogi.today.state.TodayRecordStateHolder
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import kotlinx.coroutines.launch

class TodayRecordReceiverService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WatchReceiver", "onMessageReceived 호출됨: ${messageEvent.path}")
        super.onMessageReceived(messageEvent)

        if (messageEvent.path == "/today_record") {
            val json = String(messageEvent.data)
            val dto = Gson().fromJson(json, ReportDto::class.java)

            // 상태 업데이트
            TodayRecordStateHolder.update(dto)

            // 로컬 저장
            saveReportToLocal(applicationContext, dto)
        }
    }

    private fun saveReportToLocal(context: Context, dto: ReportDto) {
        // suspend 함수는 background thread에서 실행해야 함
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            saveReportCache(context, dto)
        }
    }
}
