package com.ddc.bansoogi.landing.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ddc.bansoogi.common.util.time.validateOrShowError
import com.ddc.bansoogi.common.util.time.validateTime
import com.ddc.bansoogi.landing.controller.LandingController
import com.ddc.bansoogi.landing.ui.component.DishTimeComponent
import com.ddc.bansoogi.landing.ui.component.DurationPicker
import com.ddc.bansoogi.landing.ui.component.NextButton
import com.ddc.bansoogi.landing.ui.component.RoundedContainerBox
import com.ddc.bansoogi.landing.ui.component.text.SubTitleText
import com.ddc.bansoogi.landing.ui.component.TimeInputTextField

@Composable
fun TimeSettingScreen(controller: LandingController, onFinish: () -> Unit) {

    val wakeUpTimeState = remember { mutableStateOf("") }
    val bedTimeState = remember { mutableStateOf("") }
    val breakfastTimeState = remember { mutableStateOf("") }
    val lunchTimeState = remember { mutableStateOf("") }
    val dinnerTimeState = remember { mutableStateOf("") }
    val durationTimeState = remember { mutableStateOf(15) }

    val breakfastEnabled = remember { mutableStateOf(false) }
    val lunchEnabled = remember { mutableStateOf(false) }
    val dinnerEnabled = remember { mutableStateOf(false) }

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column {
        RoundedContainerBox(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        SubTitleText("기상 희망 시간")
                        Spacer(Modifier.padding(4.dp))
                        Column() {
                            TimeInputTextField(
                                timeState = wakeUpTimeState,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Spacer(Modifier.padding(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        SubTitleText("취침 희망 시간")
                        Spacer(Modifier.padding(4.dp))
                        Column() {
                            TimeInputTextField(timeState = bedTimeState)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                Column {
                    SubTitleText("식사 희망 시간")
                    Spacer(Modifier.padding(4.dp))
                    Row {
                        DishTimeComponent(
                            title = "아침",
                            timeState = breakfastTimeState,
                            onToggleChanged = {
                                breakfastEnabled.value = it
                            },
                            onTimeChanged = { time ->
                            }
                        )
                    }
                    Spacer(Modifier.padding(4.dp))
                    Row {
                        DishTimeComponent(
                            title = "점심",
                            timeState = lunchTimeState,
                            onToggleChanged = {
                                lunchEnabled.value = it
                            },
                            onTimeChanged = { time ->
                            }
                        )
                    }
                    Spacer(Modifier.padding(4.dp))
                    Row {
                        DishTimeComponent(
                            title = "저녁",
                            timeState = dinnerTimeState,
                            onToggleChanged = {
                                dinnerEnabled.value = it
                            },
                            onTimeChanged = { time ->
                            }
                        )
                    }
                    Spacer(Modifier.padding(4.dp))
                    Row {
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    SubTitleText("상태 지속 시간")
                    Spacer(Modifier.weight(1f))
                    DurationPicker(durationState = durationTimeState)
                }
            }
        }

        NextButton(
            enabled =
                isAllTimeValid(
                    wakeUp = wakeUpTimeState.value,
                    bedTime = bedTimeState.value,
                    breakfast = breakfastTimeState.value,
                    breakfastEnabled = breakfastEnabled.value,
                    lunch = lunchTimeState.value,
                    lunchEnabled = lunchEnabled.value,
                    dinner = dinnerTimeState.value,
                    dinnerEnabled = dinnerEnabled.value
                )
            , onClick = {
                val requiredValid = listOf(
                    Pair(wakeUpTimeState, "기상 희망 시간"),
                    Pair(bedTimeState, "취침 희망 시간"),
                )

                val optionalValid = listOfNotNull(
                    if (breakfastEnabled.value) Pair(breakfastTimeState, "아침 식사 시간") else null,
                    if (lunchEnabled.value) Pair(lunchTimeState, "점심 식사 시간") else null,
                    if (dinnerEnabled.value) Pair(dinnerTimeState, "저녁 식사 시간") else null,
                )

                val allValid = (requiredValid + optionalValid).all { (state, label) ->
                    validateOrShowError(
                        state, label,
                        setError = { errorMessage = it },
                        onInvalid = { showErrorDialog = true }
                    )
                }

                if (allValid) {

                    controller.timeSettingModel.wakeUpTime
                    controller.timeSettingModel.bedTimeTime
                    controller.timeSettingModel.breakfastTime
                    controller.timeSettingModel.lunchTime
                    controller.timeSettingModel.dinnerTime
                    controller.timeSettingModel.durationMinutes

                    onFinish()
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = {
                    showErrorDialog = false
                },
                confirmButton = {
                    Text(
                        "확인",
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { showErrorDialog = false }
                    )
                },
                title = { Text("입력 오류") },
                text = { Text(errorMessage) }
            )
        }
    }
}

// 내부로직
@Composable
private fun isAllTimeValid(
    wakeUp: String,
    bedTime: String,
    breakfast: String,
    breakfastEnabled: Boolean,
    lunch: String,
    lunchEnabled: Boolean,
    dinner: String,
    dinnerEnabled: Boolean
): Boolean {
    val alwaysRequired = listOf(wakeUp, bedTime)
    val conditionallyRequired = listOfNotNull(
        if (breakfastEnabled) breakfast else null,
        if (lunchEnabled) lunch else null,
        if (dinnerEnabled) dinner else null,
    )

    val allTimes = alwaysRequired + conditionallyRequired

    return allTimes.all { validateTime(it.filter { ch -> ch.isDigit() }) }
}