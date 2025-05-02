package com.example.eggi.calendar.controller

import com.example.eggi.calendar.data.model.DetailReport
import com.example.eggi.calendar.data.model.RecordedReportModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class RecordedController {
    private val model = RecordedReportModel()
    private val scope = CoroutineScope(Dispatchers.Main)

    suspend fun getDetailReport(date: String): DetailReport? {
        return model.getDetailReport(date)
    }
}