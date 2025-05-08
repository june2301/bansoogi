package com.ddc.bansoogi.landing.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddc.bansoogi.landing.controller.LandingController
import com.ddc.bansoogi.landing.ui.component.DefaultBodyText
import com.ddc.bansoogi.landing.ui.component.DefaultTitleText
import com.ddc.bansoogi.landing.ui.component.DropdownSelector
import com.ddc.bansoogi.landing.ui.component.NextButton
import com.ddc.bansoogi.landing.ui.component.RoundedContainerBox
import java.util.Calendar

// TODO: 
//  29일을 설정하고 윤달이 아닌 달을 해도 똑같이 적용되는 부분 수정
//  오늘 이후의 날짜에 대해서도 열려있기 때문에 수정 필요
@Composable
fun BirthInputScreen(controller: LandingController, onNext: () -> Unit) {

    val calendar = Calendar.getInstance().apply {
        time = controller.profileModel.birthDate
    }

    val selectedYear = remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    val selectedMonth = remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    val selectedDay = remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    val yearList = (1940..2025).toList().reversed()
    val monthList = (1..12).toList()
    val dayList = remember(selectedYear.value, selectedMonth.value) {
        val maxDay = when (selectedMonth.value) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(selectedYear.value)) 29 else 28
            else -> 30
        }
        (1..maxDay).toList()
    }

    Column {
        RoundedContainerBox {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                DefaultTitleText("프로필 정보")
                Spacer(modifier = Modifier.width(4.dp))
                DefaultBodyText("생년월일을 입력해 주세요")

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownSelector("년", yearList, selectedYear)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("년", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

                    Spacer(modifier = Modifier.width(16.dp))

                    DropdownSelector("월", monthList, selectedMonth)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("월", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

                    Spacer(modifier = Modifier.width(16.dp))

                    DropdownSelector("일", dayList, selectedDay)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("일", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        NextButton(
            onClick = {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear.value)
                    set(Calendar.MONTH, selectedMonth.value)
                    set(Calendar.DAY_OF_MONTH, selectedDay.value)
                }
                controller.profileModel.birthDate = calendar.time
                onNext()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
