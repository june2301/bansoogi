package com.example.eggi.calendar.data.model

import java.time.LocalDate

data class HistoryItemDto (
    val date: LocalDate,
    val bansoogiAnimationId: Int?
)