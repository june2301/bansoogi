package com.ddc.bansoogi.calendar.controller

import com.ddc.bansoogi.calendar.data.model.DetailReportDto
import com.ddc.bansoogi.calendar.data.model.RecordedReportModel
import com.ddc.bansoogi.common.data.model.TodayRecordDto

class RecordedController {
    private val model = RecordedReportModel()

    fun getDetailReport(date: String): DetailReportDto? {
        return model.getDetailReport(date)
    }

    fun getLatestRecordedReport(): DetailReportDto {
        return model.getLatestRecordedReport()
    }

    suspend fun createRecordedReport(
        todayRecordDto: TodayRecordDto,
        bansoogiIdData: Int,
        walkCountData: Int,
        stairsClimbedData: Int,
        sleepTimeData: Int,
        exerciseTimeData: Int
    ) {
        model.createRecordedReport(
            todayRecordDto,
            bansoogiIdData,
            walkCountData,
            stairsClimbedData,
            sleepTimeData,
            exerciseTimeData
        )
    }
}