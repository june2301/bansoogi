package com.example.eggi.calendar.view

import com.example.eggi.calendar.data.model.DetailReport
import com.example.eggi.calendar.data.model.HistoryItem

interface CalendarView {
    fun displayCalendar(history: List<HistoryItem>)
}