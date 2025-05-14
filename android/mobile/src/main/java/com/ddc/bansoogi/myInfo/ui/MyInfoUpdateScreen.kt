package com.ddc.bansoogi.myInfo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ddc.bansoogi.collection.data.model.CollectionModel
import com.ddc.bansoogi.myInfo.controller.MyInfoController
import com.ddc.bansoogi.myInfo.controller.MyInfoUpdateController
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto
import kotlinx.coroutines.launch
import com.ddc.bansoogi.common.notification.AlarmScheduler
import com.ddc.bansoogi.common.notification.SyncHelper
import com.ddc.bansoogi.landing.ui.component.NicknameTextField
import com.ddc.bansoogi.landing.ui.component.DropdownSelector
import com.ddc.bansoogi.landing.ui.screen.isLeapYear
import com.ddc.bansoogi.landing.ui.component.TimeInputTextField
import com.ddc.bansoogi.landing.ui.component.DishTimeComponent
import com.ddc.bansoogi.landing.ui.component.DurationPicker

import java.util.*

@Composable
fun MyInfoUpdateScreen(navController: NavController) {

    val controller = remember { MyInfoController() }

    val myInfo by controller.myInfoFlow()
        .collectAsState(initial = null)

    myInfo?.let { initial ->
        val updateController = remember {
            MyInfoUpdateController { _ ->
                navController.popBackStack()
            }
        }

        val context = LocalContext.current
        val collectionModel = remember { CollectionModel() }

        var profileBansoogiId by remember { mutableStateOf(initial.profileBansoogiId) }

        val imageResId = remember(profileBansoogiId) {
            collectionModel.getImageResId(context, profileBansoogiId)
        }

        val nickname = remember { mutableStateOf(initial.nickname) }
        var isNicknameValid by remember { mutableStateOf(true) }

        val calendarNow = remember { Calendar.getInstance() }
        val initParts   = initial.birthDate.split(".").mapNotNull { it.toIntOrNull() }

        val initYear  = initParts.getOrNull(0) ?: 2000
        val initMonth = initParts.getOrNull(1) ?: 1
        val initDay   = initParts.getOrNull(2) ?: 1

        val selectedYear  = remember { mutableStateOf(initYear) }
        val selectedMonth = remember { mutableStateOf(initMonth) }
        val selectedDay   = remember { mutableStateOf(initDay) }

        val yearOptions   = (1940..calendarNow.get(Calendar.YEAR)).toList().reversed()
        val monthOptions  = (1..12).toList()

        val dayOptions by remember(selectedYear.value, selectedMonth.value) {
            val maxDay = when (selectedMonth.value) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11           -> 30
                2 -> if (isLeapYear(selectedYear.value)) 29 else 28
                else -> 30
            }
            mutableStateOf((1..maxDay).toList())
        }

        if (selectedDay.value !in dayOptions) {
            selectedDay.value = dayOptions.last()
        }

        val wakeUpTime = remember { mutableStateOf(initial.wakeUpTime) }
        val sleepTime    = remember { mutableStateOf(initial.sleepTime) }

        val breakfastTime = remember { mutableStateOf(initial.breakfastTime) }
        val breakfastEnabled   = remember { mutableStateOf(initial.breakfastTime.isNotBlank()) }
        val lunchTime = remember { mutableStateOf(initial.lunchTime) }
        val lunchEnabled   = remember { mutableStateOf(initial.lunchTime.isNotBlank()) }
        val dinnerTime = remember { mutableStateOf(initial.dinnerTime) }
        val dinnerEnabled   = remember { mutableStateOf(initial.dinnerTime.isNotBlank()) }

        val notificationDuration = remember {
            mutableStateOf(initial.notificationDuration.coerceIn(15, 180))
        }

        var showResetButton by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { showResetButton = !showResetButton },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
                )

                if (showResetButton) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                profileBansoogiId = 1
                                showResetButton = false
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color(0xAA000000),
                            contentColor = Color.White
                        )
                    ) {
                        Text("사진 초기화")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "닉네임",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.height(8.dp))

                NicknameTextField(
                    letterCount         = 8,
                    nicknameState       = nickname,
                    onValidationChanged = { isValid -> isNicknameValid = isValid },
                    modifier            = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "생년월일",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DropdownSelector("년", yearOptions, selectedYear)
                        Spacer(Modifier.width(4.dp))
                        Text("년", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.width(16.dp))

                        DropdownSelector("월", monthOptions, selectedMonth)
                        Spacer(Modifier.width(4.dp))
                        Text("월", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.width(16.dp))

                        DropdownSelector("일", dayOptions, selectedDay)
                        Spacer(Modifier.width(4.dp))
                        Text("일", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Divider(thickness = 2.dp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "기상 희망 시간",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))

                    TimeInputTextField(
                        timeState = wakeUpTime,
                        modifier  = Modifier.width(120.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "취침 희망 시간",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))

                    TimeInputTextField(
                        timeState = sleepTime,
                        modifier  = Modifier.width(120.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Divider(thickness = 2.dp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("식사 희망 시간", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                DishTimeComponent(
                    title              = "아침",
                    initialToggleState = breakfastEnabled.value,
                    timeState          = breakfastTime,
                    onToggleChanged    = { enabled ->
                        breakfastEnabled.value = enabled
                        if (!enabled) breakfastTime.value = ""
                    },
                    onTimeChanged      = { }
                )
                Spacer(Modifier.height(8.dp))

                DishTimeComponent(
                    title              = "점심",
                    initialToggleState = lunchEnabled.value,
                    timeState          = lunchTime,
                    onToggleChanged    = { enabled ->
                        lunchEnabled.value = enabled
                        if (!enabled) lunchTime.value = ""
                    },
                    onTimeChanged      = { }
                )
                Spacer(Modifier.height(8.dp))

                DishTimeComponent(
                    title              = "저녁",
                    initialToggleState = dinnerEnabled.value,
                    timeState          = dinnerTime,
                    onToggleChanged    = { enabled ->
                        dinnerEnabled.value = enabled
                        if (!enabled) dinnerTime.value = ""
                    },
                    onTimeChanged      = { }
                )
            }
            Spacer(Modifier.height(16.dp))
            Divider(thickness = 2.dp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("상태 지속 시간", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))

                DurationPicker(
                    durationState = notificationDuration,
                    min   = 15,
                    max   = 180,
                    step  = 5
                )

                Spacer(Modifier.width(4.dp))
                Text("분", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFFFFF),
                        contentColor = Color(0xFF888888)
                    ),
                    modifier = Modifier
                        .height(48.dp)
                ) {
                    Text(
                        text = "취소",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = {
                        val birthDate = "%04d.%02d.%02d".format(
                            selectedYear.value, selectedMonth.value, selectedDay.value
                        )
                        val newInfo = MyInfoDto(
                            userId               = initial.userId,
                            nickname             = nickname.value,
                            birthDate            = birthDate,
                            profileBansoogiId    = profileBansoogiId,
                            wakeUpTime           = wakeUpTime.value,
                            sleepTime            = sleepTime.value,
                            breakfastTime        = if (breakfastEnabled.value) breakfastTime.value else "",
                            lunchTime            = if (lunchEnabled.value)     lunchTime.value     else "",
                            dinnerTime           = if (dinnerEnabled.value)    dinnerTime.value    else "",
                            notificationDuration = notificationDuration.value,
                            notificationEnabled  = initial.notificationEnabled,
                            bgSoundEnabled       = initial.bgSoundEnabled,
                            effectSoundEnabled   = initial.effectSoundEnabled
                        )
                        updateController.save(newInfo)

                        AlarmScheduler.rescheduleAllDailyAlarms(context, newInfo)
                        SyncHelper.syncNotificationToWatch(context, newInfo)
                    },
                    enabled   = isNicknameValid,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00CB1E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                ) {
                    Text(
                        text = "저장하기",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    } ?: Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("로딩 중...", fontSize = 16.sp)
    }
}

fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)