package com.ddc.bansoogi.calendar.data.model

import java.time.LocalDate

data class CalendarMarkerDto (
    val date: LocalDate,
    val bansoogiAnimationId: Int?
)