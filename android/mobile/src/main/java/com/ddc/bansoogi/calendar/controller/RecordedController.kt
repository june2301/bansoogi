package com.ddc.bansoogi.calendar.controller

import com.ddc.bansoogi.calendar.data.model.DetailReportDto
import com.ddc.bansoogi.calendar.data.model.RecordedReportModel
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class RecordedController {
    private val model = RecordedReportModel()
    private val scope = CoroutineScope(Dispatchers.Main)

    suspend fun getDetailReport(date: String): DetailReportDto? {
        return model.getDetailReport(date)
    }

    suspend fun createRecordedReport(
        todayRecordDto: TodayRecordDto,
        bansoogiIdData: Int,
        walkCountData: Int,
        runTimeData: Int,
        exerciseTimeData: Int,
        stairsClimbedData: Int
    ) {
        model.createRecordedReport(
            todayRecordDto,
            bansoogiIdData,
            walkCountData,
            runTimeData,
            exerciseTimeData,
            stairsClimbedData
        )
    }
}