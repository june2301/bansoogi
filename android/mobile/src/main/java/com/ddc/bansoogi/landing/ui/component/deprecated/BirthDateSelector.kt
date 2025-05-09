//package com.ddc.bansoogi.landing.ui.component
//
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.height
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.MutableState
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//
//@Composable
//fun BirthDateSelector(
//    selectedYear: MutableState<Int>,
//    selectedMonth: MutableState<Int>,
//    selectedDay: MutableState<Int>
//) {
//    val yearList = (1900..2025).toList().reversed()
//    val monthList = (1..12).toList()
//    val dayList = remember(selectedYear.value, selectedMonth.value) {
//        val maxDay = when (selectedMonth.value) {
//            1, 3, 5, 7, 8, 10, 12 -> 31
//            4, 6, 9, 11 -> 30
//            2 -> if (isLeapYear(selectedYear.value)) 29 else 28
//            else -> 30
//        }
//        (1..maxDay).toList()
//    }
//
//    Column {
//        Text("생년월일", style = MaterialTheme.typography.titleMedium)
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//            DropdownSelector("년도", yearList, selectedYear)
//            DropdownSelector("월", monthList, selectedMonth)
//            DropdownSelector("일", dayList, selectedDay)
//        }
//    }
//}
//
//fun isLeapYear(year: Int): Boolean {
//    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
//}