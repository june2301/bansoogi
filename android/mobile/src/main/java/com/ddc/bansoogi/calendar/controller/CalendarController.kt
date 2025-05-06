package com.ddc.bansoogi.calendar.controller

import com.ddc.bansoogi.calendar.data.model.RecordedReportModel
import com.ddc.bansoogi.calendar.view.CalendarView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CalendarController(private val view: CalendarView) {
    private val model = RecordedReportModel()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        scope.launch {
            model.initialize() // 더미데이터 설정

            // 데이터를 화면에 표시
            model.getCalendarMarkers().collectLatest { markers ->
                view.displayCalendar(markers)
            }
        }
    }
}