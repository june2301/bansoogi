package com.ddc.bansoogi.common.mobile.communication.receiver

import android.content.Context
import com.ddc.bansoogi.common.mobile.data.mapper.JsonMapper
import com.ddc.bansoogi.myinfo.data.dto.MyInfoDto
import com.ddc.bansoogi.myinfo.data.store.saveMyInfoCache
import com.ddc.bansoogi.myinfo.state.MyInfoStateHolder
import com.ddc.bansoogi.today.data.dto.ReportDto
import com.ddc.bansoogi.today.data.store.saveReportCache
import com.ddc.bansoogi.today.data.store.updateEnergyCache
import com.ddc.bansoogi.today.state.TodayRecordStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RequestHandler(
    private val context: Context,
    private val scope: CoroutineScope
) {
    fun handleEnergyData(data: ByteArray) {
        handleData (
            data = data,
            deserialize = { JsonMapper.fromJson<Int>(String(it)) },
            updateState = { TodayRecordStateHolder.updateEnergy(it) },
            saveToLocal = { updateEnergyCache(context, it) },
            scope = scope
        )
    }

    fun handleTodayRecordData(data: ByteArray) {
        handleData (
            data = data,
            deserialize = { JsonMapper.fromJson<ReportDto>(String(it)) },
            updateState = { TodayRecordStateHolder.update(it) },
            saveToLocal = { saveReportCache(context, it) },
            scope = scope
        )
    }

    fun handleMyInfoData(data: ByteArray) {
        handleData (
            data = data,
            deserialize = { JsonMapper.fromJson<MyInfoDto>(String(it)) },
            updateState = { MyInfoStateHolder.update(it) },
            saveToLocal = { saveMyInfoCache(context, it) },
            scope = scope
        )
    }
}

private inline fun <T> handleData(
    data: ByteArray,
    crossinline deserialize: (ByteArray) -> T,
    crossinline updateState: (T) -> Unit,
    crossinline saveToLocal: suspend (T) -> Unit,
    scope: CoroutineScope
) {
    // 수신한 바이트 데이터 -> String(Json) -> 객체
    val deserializedData  = deserialize(data)

    // 상태 업데이트
    updateState(deserializedData)

    // 로컬 저장
    scope.launch {
        saveToLocal(deserializedData)
    }
}