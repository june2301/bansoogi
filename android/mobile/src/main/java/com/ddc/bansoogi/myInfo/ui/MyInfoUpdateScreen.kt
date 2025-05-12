package com.ddc.bansoogi.myInfo.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
import java.util.*

@Composable
fun MyInfoUpdateScreen(navController: NavController) {

    val controller = remember { MyInfoController() }

    val myInfo by controller.myInfoFlow()
        .collectAsState(initial = null)

    val context = LocalContext.current
    LaunchedEffect(Unit) { controller.initialize(context) }

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

        var nickname by remember { mutableStateOf(initial.nickname) }
        var birthDate by remember { mutableStateOf(TextFieldValue(initial.birthDate)) }
        var wakeUpTime by remember { mutableStateOf(TextFieldValue(initial.wakeUpTime)) }
        var sleepTime by remember { mutableStateOf(TextFieldValue(initial.sleepTime)) }

        val calendar = Calendar.getInstance().apply {
            initial.birthDate.split(".")
                .mapNotNull { it.toIntOrNull() }
                .also {
                    if (it.size == 3) {
                        set(it[0], it[1] - 1, it[2])
                    }
                }
        }
        val datePicker = remember {
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    val formatted = "%04d.%02d.%02d".format(y, m + 1, d)
                    birthDate = TextFieldValue(formatted, selection = TextRange(formatted.length))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        val defaultMeals = listOf("아침", "점심", "저녁", "야식")
        val mealOrder = remember {
            mutableStateListOf<String>().apply {
                if (initial.breakfastTime.isNotBlank()) add("아침")
                if (initial.lunchTime.isNotBlank())     add("점심")
                if (initial.dinnerTime.isNotBlank())    add("저녁")
            }
        }
        val timesMap = remember {
            mutableStateMapOf<String, MutableState<TextFieldValue>>().apply {
                listOf("아침","점심","저녁").forEach { label ->
                    val initialTime = when(label){
                        "아침" -> initial.breakfastTime
                        "점심" -> initial.lunchTime
                        else    -> initial.dinnerTime
                    }
                    this[label] = mutableStateOf(TextFieldValue(initialTime.ifBlank { "" }))
                }
            }
        }
        fun addMeal() {
            defaultMeals.firstOrNull { it !in mealOrder }?.let { label ->
                val idx = defaultMeals.indexOf(label)
                mealOrder.add(idx, label)
                timesMap[label] = mutableStateOf(TextFieldValue(""))
            }
        }
        fun removeMeal(label: String) {
            timesMap.remove(label)
            mealOrder.remove(label)
        }

        var showResetButton by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        var notificationDuration by remember { mutableStateOf(initial.notificationDuration) }
        fun incDuration() { notificationDuration += 15 }
        fun decDuration() { if (notificationDuration > 15) notificationDuration -= 15 }

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

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = { Text("닉네임", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888)) },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = birthDate,
                onValueChange = { input ->
                    val digits = input.text.filter { it.isDigit() }.take(8)

                    val formatted = when (digits.length) {
                        in 0..4 -> digits
                        in 5..6 -> "${digits.substring(0, 4)}.${digits.substring(4)}"
                        in 7..8 -> "${digits.substring(0, 4)}.${digits.substring(4, 6)}.${digits.substring(6)}"
                        else -> birthDate.text
                    }

                    birthDate = TextFieldValue(
                        text = formatted,
                        selection = TextRange(formatted.length)
                    )
                },
                readOnly = false,
                placeholder = {
                    Text("YYYY.MM.DD", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        val parts = birthDate.text.split(".").mapNotNull { it.toIntOrNull() }
                        if (parts.size == 3) {
                            calendar.set(parts[0], parts[1] - 1, parts[2])
                        }
                        datePicker.show()
                    }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "달력")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("기상 희망 시간", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = wakeUpTime,
                    onValueChange = { input ->
                        val digits = input.text.filter { it.isDigit() }.take(4)
                        val formatted = when (digits.length) {
                            0 -> ""
                            in 1..2 -> digits
                            in 3..4 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
                            else -> wakeUpTime.text
                        }

                        wakeUpTime = TextFieldValue(
                            text = formatted,
                            selection = TextRange(formatted.length)
                        )
                    },
                    placeholder = {
                        Text("00:00", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
                    },
                    singleLine = true,
                    modifier = Modifier.width(100.dp).defaultMinSize(minHeight = 48.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("취침 희망 시간", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = sleepTime,
                    onValueChange = { input ->
                        val digits = input.text.filter { it.isDigit() }.take(4)
                        val formatted = when (digits.length) {
                            0 -> ""
                            in 1..2 -> digits
                            in 3..4 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
                            else -> sleepTime.text
                        }

                        sleepTime = TextFieldValue(
                            text = formatted,
                            selection = TextRange(formatted.length)
                        )
                    },
                    placeholder = {
                        Text("00:00", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
                    },
                    singleLine = true,
                    modifier = Modifier.width(100.dp).defaultMinSize(minHeight = 48.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(Modifier.height(12.dp))
            Divider(thickness = 2.dp, color = Color.Gray)
            Spacer(Modifier.height(18.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(end = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("식사 희망 시간", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF5ECFFF), CircleShape)
                        .clickable { if (mealOrder.size < 4) addMeal() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "추가",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            mealOrder.forEach { label ->
                val state = timesMap[label] ?: return@forEach
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.value,
                            onValueChange = { input ->
                                val digits = input.text.filter { it.isDigit() }.take(4)
                                val formatted = when (digits.length) {
                                    0 -> ""
                                    in 1..2 -> digits
                                    in 3..4 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
                                    else -> input.text
                                }
                                state.value = TextFieldValue(
                                    text = formatted,
                                    selection = TextRange(formatted.length)
                                )
                            },
                            placeholder = {
                                Text("00:00", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .width(100.dp)
                                .padding(end = 4.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFF8370), CircleShape)
                                .clickable { removeMeal(label) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "제거",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(thickness = 2.dp, color = Color.Gray)
            Spacer(Modifier.height(18.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("상태 지속 시간", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color.Black,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFF8370), CircleShape)
                            .clickable { decDuration() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "감소",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "$notificationDuration",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "분",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF888888),
                        style = LocalTextStyle.current.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF5ECFFF), CircleShape)
                            .clickable { incDuration() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "증가",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
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
                        val newInfo = MyInfoDto(
                            userId               = initial.userId,
                            nickname             = nickname,
                            birthDate            = birthDate.text,
                            profileBansoogiId    = profileBansoogiId,
                            wakeUpTime           = wakeUpTime.text,
                            sleepTime            = sleepTime.text,
                            breakfastTime        = timesMap["아침"]?.value?.text.orEmpty(),
                            lunchTime            = timesMap["점심"]?.value?.text.orEmpty(),
                            dinnerTime           = timesMap["저녁"]?.value?.text.orEmpty(),
                            notificationDuration = notificationDuration,
                            notificationEnabled  = initial.notificationEnabled,
                            bgSoundEnabled       = initial.bgSoundEnabled,
                            effectSoundEnabled   = initial.effectSoundEnabled
                        )
                        updateController.save(newInfo)

                        AlarmScheduler.scheduleAllDailyAlarms(context, newInfo)
                        SyncHelper.syncNotificationToWatch(context, newInfo)
                    },
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
