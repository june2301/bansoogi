package com.ddc.bansoogi.calendar.view

import com.ddc.bansoogi.calendar.data.model.CalendarMarkerDto

interface CalendarView {
    fun displayCalendar(calendarMarkers: List<CalendarMarkerDto>)
}