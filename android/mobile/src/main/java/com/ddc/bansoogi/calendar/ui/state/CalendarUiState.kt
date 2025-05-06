package com.ddc.bansoogi.calendar.ui.state

import androidx.compose.runtime.Stable
import com.ddc.bansoogi.calendar.data.model.CalendarMarkerDto
import java.time.LocalDate

@Stable
data class CalendarUiState (
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val calendarMarkers: List<CalendarMarkerDto> = emptyList()
)