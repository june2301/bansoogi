package com.ddc.bansoogi.today.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.ddc.bansoogi.today.data.dto.ReportDto

object TodayRecordStateHolder {
    var reportDto by mutableStateOf<ReportDto?>(null)

    fun update(newDto: ReportDto?) {
        reportDto = newDto ?: ReportDto.default()
    }
}