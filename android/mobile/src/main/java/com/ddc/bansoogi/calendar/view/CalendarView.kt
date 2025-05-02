package com.ddc.bansoogi.calendar.view

import com.ddc.bansoogi.calendar.data.model.HistoryItemDto

interface CalendarView {
    fun displayCalendar(history: List<HistoryItemDto>)
}