package com.ddc.bansoogi.common.wear.communication.receiver

import android.content.Context
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.wear.communication.sender.WearMyInfoSender
import com.ddc.bansoogi.common.wear.communication.sender.WearTodayRecordSender
import com.ddc.bansoogi.common.wear.data.mapper.WearDtoMapper
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RequestHandler(
    private val context: Context,
    private val scope: CoroutineScope
) {
    fun handleEnergyRequest() {
        handleDataRequest(
            getData = { TodayRecordModel().getTodayRecordSync() },
            mapData = { WearDtoMapper.toEnergy(it) },
            sendData = { WearTodayRecordSender.sendEnergy(context, it) },
            scope = scope
        )
    }

    fun handleTodayRecordRequest() {
        handleDataRequest(
            getData = { TodayRecordModel().getTodayRecordSync() },
            mapData = { WearDtoMapper.toWearReport(it) },
            sendData = { WearTodayRecordSender.send(context, it) },
            scope = scope
        )
    }

    fun handleMyInfoRequest() {
        handleDataRequest(
            getData = { MyInfoModel().getMyInfoSync() },
            mapData = { WearDtoMapper.toWearMyInfo(it) },
            sendData = { WearMyInfoSender.send(context, it) },
            scope = scope
        )
    }
}

// 데이터 처리를 위한 공통 로직
private inline fun <T, R> handleDataRequest(
    crossinline getData: () -> T?,
    crossinline mapData: (T) -> R,
    crossinline sendData: (R) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        // 데이터 조회
        getData()?.let {
            // 데이터 변환
            val mapped = mapData(it)
            
            // 데이터 전송
            sendData(mapped)
        }
    }
}