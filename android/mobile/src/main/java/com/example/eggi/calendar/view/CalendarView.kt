package com.example.eggi.calendar.view

import com.example.eggi.calendar.data.model.HistoryItemDto

interface CalendarView {
    fun displayCalendar(history: List<HistoryItemDto>)
}