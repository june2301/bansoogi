package com.ddc.bansoogi.calendar.data.model

import java.time.LocalDate

data class CalendarMarkerDto (
    val date: LocalDate,
    val bansoogiGifUrl: String?,
    val bansoogiImageUrl: String?
)