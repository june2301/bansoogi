package com.ddc.bansoogi.landing.ui.component.deprecated

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Deprecated("달력 미사용으로 읜한 Deprecated")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    selectedDate: String?,
    onClickCancel: () -> Unit,
    onClickConfirm: (yyyyMMdd: String) -> Unit
) {
    DatePickerDialog (
        onDismissRequest = { onClickCancel() },
        confirmButton = {},
        colors = DatePickerDefaults.colors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        val datePickerState = rememberDatePickerState(
            yearRange = 1930..2024,
            initialDisplayMode = DisplayMode.Picker,
            initialSelectedDateMillis = selectedDate?.let {
                val formatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply {
                    // initialSelectedDateMillis는 UTC 시간을 받고 있다.
                    // 아래처럼 timeZone을 추가해서 UTC 시간으로 설정해야한다.
                    // 한국에서 실행한다면 -9시간을 적용하기 때문이다.
                    //
                    // 만약에 아래 코드가 없다면 20240228을 넘겼을 때, 안드로이드는 KST 20240228000000 으로 인식할 것이고,
                    // 이를 UTC 시간으로 변환하면서 -9시간을 적용하기 때문에 결과적으로 20240228을 넘기면 2024년 2월 27일에 선택이 되있는 문제가 발생한다.
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                formatter.parse(it)?.time
                    ?: System.currentTimeMillis() // 날짜 파싱 실패 시 현재 시간을 기본값으로 사용
            } ?: System.currentTimeMillis(), // selectedDate가 null인 경우 현재 시간을 기본값으로 사용,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // 날짜 제한 조건
                    return true
//                    return utcTimeMillis > System.currentTimeMillis()
                }
            })

        DatePicker(
            state = datePickerState,
        )

        Spacer(modifier = Modifier.width(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button (onClick = {
                onClickCancel()
            }) {
                Text(text = "취소")
            }

            Spacer(modifier = Modifier.width(5.dp))

            Button(onClick = {
                datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                    val yyyyMMdd = SimpleDateFormat(
                        "yyyyMMdd",
                        Locale.getDefault()
                    ).format(Date(selectedDateMillis))

                    onClickConfirm(yyyyMMdd)
                }
            }) {
                Text(text = "확인")
            }
        }
    }
}

// CustomDatePickerDialogState.kt
data class CustomDatePickerDialogState(
    var selectedDate: String? = null,
    var isShowDialog: Boolean = false,
    val onClickConfirm: (yyyyMMdd: String) -> Unit = {},
    val onClickCancel: () -> Unit = {},
)

/*
 // 사용방법

 상단에 선언
     val datePickerState = remember {
        mutableStateOf(CustomDatePickerDialogState())
    }

button 클릭을 시작으로 picker start
                Button (onClick = {
                    datePickerState.value = CustomDatePickerDialogState(
                        selectedDate = "20240228", // 또는 초기 날짜 (예: "20240228")
                        isShowDialog = true,
                        onClickConfirm = { yyyyMMdd ->
                            datePickerState.value = datePickerState.value.copy(
                                selectedDate = yyyyMMdd,
                                isShowDialog = false
                            )
                        },
                        onClickCancel = {
                            datePickerState.value = datePickerState.value.copy(
                                isShowDialog = false
                            )
                        }
                    )
                }) {
                    Text("날짜 선택")
                }

                if (datePickerState.value.isShowDialog) {
                    CustomDatePickerDialog(
                        selectedDate = datePickerState.value.selectedDate,
                        onClickCancel = datePickerState.value.onClickCancel,
                        onClickConfirm = datePickerState.value.onClickConfirm
                    )
                }

                Text(
                    text = datePickerState.value.selectedDate ?: "날짜를 선택하세요",
                    style = MaterialTheme.typography.bodyLarge
                )
 */