package com.ddc.bansoogi.calendar.ui.state

import androidx.compose.runtime.Stable
import java.time.LocalDate

@Stable
data class CalendarContentUiState(
    val currentViewDate: LocalDate,
    val selectedDate: String? = null,
    val showModal: Boolean = false
)
