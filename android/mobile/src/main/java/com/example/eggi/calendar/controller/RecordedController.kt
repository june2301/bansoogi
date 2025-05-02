package com.example.eggi.calendar.controller

import com.example.eggi.calendar.data.model.DetailReportDto
import com.example.eggi.calendar.data.model.RecordedReportModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class RecordedController {
    private val model = RecordedReportModel()
    private val scope = CoroutineScope(Dispatchers.Main)

    suspend fun getDetailReport(date: String): DetailReportDto? {
        return model.getDetailReport(date)
    }
}