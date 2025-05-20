package com.ddc.bansoogi.common.wear.communication.processor

import com.ddc.bansoogi.common.data.model.TodayRecordModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object StaticEventProcessor {
    private val gson = com.google.gson.Gson()

    private data class WarnDto(val type: String, val duration: Int)
    private data class BreakDto(val type: String, val energy: Int)
    private data class AccumDto(val lying: Int?, val sitting: Int?)

    fun handleWarn(scope: CoroutineScope, raw: ByteArray) {
        val dto = gson.fromJson(String(raw), WarnDto::class.java)
        scope.launch {
            val model = TodayRecordModel(); val rec = model.getTodayRecordSync() ?: return@launch
            when (dto.type) {
                "SITTING_LONG" -> model.dataSource.updateSittingTime(rec.recordId, dto.duration)
                "LYING_LONG"   -> model.dataSource.updateLyingTime(rec.recordId, dto.duration)
            }
            model.logModel.createActivicyLog(dto.type, "STATIC", dto.duration)
        }
    }

    fun handleBreak(scope: CoroutineScope, raw: ByteArray) {
        val dto = gson.fromJson(String(raw), BreakDto::class.java)
        scope.launch {
            val model = TodayRecordModel(); val rec = model.getTodayRecordSync() ?: return@launch
            model.dataSource.updateEnergy(rec.recordId, dto.energy)
            when (dto.type) {
                "STRETCH_REWARD" -> model.dataSource.updateStretchCnt(rec.recordId)
                "STANDUP_REWARD" -> model.dataSource.updateStandUpCnt(rec.recordId)
            }
            model.logModel.createActivicyLog(dto.type, "BREAK_STATIC", 0)
        }
    }

    fun handleAccum(scope: CoroutineScope, raw: ByteArray) {
        val dto = gson.fromJson(String(raw), AccumDto::class.java)
        scope.launch {
            val model = TodayRecordModel(); val rec = model.getTodayRecordSync() ?: return@launch
            dto.lying?.let { model.dataSource.updateLyingTime(rec.recordId, it) }
            dto.sitting?.let { model.dataSource.updateSittingTime(rec.recordId, it) }
        }
    }
}