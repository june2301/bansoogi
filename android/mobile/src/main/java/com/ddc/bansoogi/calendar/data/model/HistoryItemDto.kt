package com.ddc.bansoogi.calendar.data.model

import java.time.LocalDate

data class HistoryItemDto (
    val date: LocalDate,
    val bansoogiAnimationId: Int?
)