package com.ddc.bansoogi.calendar.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@Stable
class YearMonthState(
    initialYear: Int,
    initialMonth: Int
) {
    var year by mutableIntStateOf(initialYear)
    var month by mutableIntStateOf(initialMonth)
}