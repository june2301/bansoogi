package com.ddc.bansoogi.common.mobile.receiver

import android.content.Context
import android.util.Log
import com.ddc.bansoogi.myinfo.data.dto.MyInfoDto
import com.ddc.bansoogi.myinfo.data.mapper.MyInfoJsonMapper
import com.ddc.bansoogi.myinfo.data.store.saveMyInfoCache
import com.ddc.bansoogi.myinfo.state.MyInfoStateHolder
import com.ddc.bansoogi.today.data.dto.ReportDto
import com.ddc.bansoogi.today.data.mapper.ReportJsonMapper
import com.ddc.bansoogi.today.data.store.saveReportCache
import com.ddc.bansoogi.today.state.TodayRecordStateHolder
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.launch

class WearReceiverService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            "/today_record" -> handleTodayRecord(messageEvent)
            "/my_info" -> handleMyInfo(messageEvent)

            else -> Log.w("WatchReceiver", "알 수 없는 경로: ${messageEvent.path}")
        }
    }

    private fun handleTodayRecord(event: MessageEvent) {
        val json = String(event.data)
        val dto = ReportJsonMapper.fromJson(json)

        // 상태 업데이트
        TodayRecordStateHolder.update(dto)

        // 로컬 저장
        saveReportToLocal(applicationContext, dto)
    }

    private fun handleMyInfo(event: MessageEvent) {
        val json = String(event.data)
        val dto = MyInfoJsonMapper.fromJson(json)

        // 상태 업데이트
        MyInfoStateHolder.update(dto)

        // 로컬 저장
        saveMyInfoToLocal(applicationContext, dto)
    }

    private fun saveReportToLocal(context: Context, dto: ReportDto) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            saveReportCache(context, dto)
        }
    }

    private fun saveMyInfoToLocal(context: Context, dto: MyInfoDto) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            saveMyInfoCache(context, dto)
        }
    }
}
