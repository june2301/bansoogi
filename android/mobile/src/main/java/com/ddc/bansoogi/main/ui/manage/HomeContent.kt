package com.ddc.bansoogi.main.ui.manage

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ddc.bansoogi.R
import com.ddc.bansoogi.calendar.ui.RecordedModal
import com.ddc.bansoogi.common.data.enum.MealType
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.main.controller.TodayRecordController
import com.ddc.bansoogi.main.ui.DayTimeModal
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import com.ddc.bansoogi.main.ui.util.InteractionUtil
import com.ddc.bansoogi.myInfo.controller.MyInfoController
import kotlinx.coroutines.delay
import java.nio.file.WatchEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.LocalTime

@Composable
fun HomeContent(
    todayRecordDto: TodayRecordDto,
    todayRecordController: TodayRecordController,
    isInSleepRange: Boolean,
    healthData: CustomHealthData,
) {
    val scope = rememberCoroutineScope()
    var showModal by remember { mutableStateOf(false) }

    val myInfoController = remember { MyInfoController() }
    val myInfo by myInfoController.myInfoFlow().collectAsState(initial = null)

    val currentTime = remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = LocalTime.now()
            delay(60_000L)
        }
    }

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val mealTimesWithType = remember(myInfo) {
        listOfNotNull(
            myInfo?.breakfastTime?.let { MealType.BREAKFAST to it },
            myInfo?.lunchTime    ?.let { MealType.LUNCH     to it },
            myInfo?.dinnerTime   ?.let { MealType.DINNER    to it }
        ).mapNotNull { (type, str) ->
            runCatching { LocalTime.parse(str, formatter) }
                .getOrNull()?.let { time -> type to time }
        }
    }

    val currentMealTypes by remember(currentTime.value, mealTimesWithType) {
        derivedStateOf {
            mealTimesWithType.filter { (_, time) ->
                val start = time.minusMinutes(30)
                val end   = time.plusMinutes(30)
                !currentTime.value.isBefore(start) && !currentTime.value.isAfter(end)
            }.map { it.first }
        }
    }

    val pendingMealTypes by remember(currentMealTypes, todayRecordDto) {
        derivedStateOf {
            currentMealTypes.filter { type ->
                when (type) {
                    MealType.BREAKFAST -> !todayRecordDto.breakfast
                    MealType.LUNCH     -> !todayRecordDto.lunch
                    MealType.DINNER    -> !todayRecordDto.dinner
                }
            }
        }
    }

    val mealEnabled = pendingMealTypes.isNotEmpty()

    // 상호 작용 애니메이션
    var triggerInteraction by remember { mutableStateOf(false) }

//    LaunchedEffect(triggerInteraction) {
//        if (triggerInteraction) {
//            // 5초 후에 showInteraction을 false로 설정하고 onFinished 콜백 호출
//            delay(5000)
//            triggerInteraction = false
//        }
//    }

    // 식사 애니메이션
    var triggerMeal by remember { mutableStateOf(false) }
//    LaunchedEffect(triggerMeal) {
//        if (triggerMeal) {
//            // 5초 후에 showInteraction을 false로 설정하고 onFinished 콜백 호출
//            delay(5000)
//            triggerMeal = false
//        }
//    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(horizontal = 16.dp)
                .background(Color.White)
                .border(
                    width = 2.dp,
                    color = Color.DarkGray
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Color(0xFFEEEEEE))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(todayRecordDto.energyPoint / 100f)
                    .height(24.dp)
                    .background(Color.Green)
            )
            Text(
                text = "${todayRecordDto.energyPoint} / 100",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 50.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val context = LocalContext.current
                val imageLoader = remember {
                    ImageLoader.Builder(context)
                        .components {
                            add(GifDecoder.Factory())
                            add(ImageDecoderDecoder.Factory())
                        }
                        .build()
                }

                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(R.drawable.ic_today)
                            .build(),
                        imageLoader = imageLoader
                    ),
                    contentDescription = "버튼 이미지",
                    modifier = Modifier
                        .width(60.dp)
                        .height(60.dp)
                        .clickable {
                            showModal = true
                        },
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "Today",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (triggerInteraction) {
                BansoogiAnimation(
                    state = BansoogiState.SMILE,
                    loop = false,
                    loopCouont = 3,
                    onAnimationEnd = {
                        triggerInteraction = false
                    }
                )
            } else if (triggerMeal) {
                BansoogiAnimation(
                    state = BansoogiState.EAT,
                    loop = false,
                    loopCouont = 1,
                    onAnimationEnd = {
                        triggerMeal = false
                    }
                )
            } else {
                BansoogiAnimation(
                    state = BansoogiStateHolder.state
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val buttonShape = RoundedCornerShape(30.dp)
        val isCoolDown = remember { mutableStateOf(true) }

        LaunchedEffect(todayRecordDto.interactionLatestTime) {
            val remainingTime = InteractionUtil.getRemainingCooldownMillis(todayRecordDto.interactionLatestTime)
            if (remainingTime > 0) {
                isCoolDown.value = true
                delay(remainingTime)
            }
            isCoolDown.value = false
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 64.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val btnModifier = Modifier
                .padding(vertical = 10.dp)
                .width(120.dp)
                .height(100.dp)
                .border(4.dp, Color.DarkGray, buttonShape)

            Button(
                onClick = {
                    triggerInteraction = true

                    todayRecordController.onInteract(todayRecordDto, isInSleepRange)
                },
                modifier = btnModifier,
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF2E616A)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "상호작용 아이콘",
                        modifier = Modifier.size(60.dp)
                    )

                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.schedule_vector),
                        contentDescription = "스케줄 아이콘",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp),
                        tint = if (isCoolDown.value || isInSleepRange) Color.Gray else Color(0xFF4CAF50)
                    )
                }

            }

            Button(
                onClick = {
                    triggerMeal = true

                    pendingMealTypes.firstOrNull()?.let { type ->
                        todayRecordController.checkMeal(todayRecordDto, type)
                    }
                },
                enabled = mealEnabled,
                modifier = btnModifier,
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = if (mealEnabled) Color(0xFF2E616A) else Color.Gray
                )
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_bread),
                        contentDescription = "식사 아이콘",
                        modifier = Modifier.size(120.dp),
                        alpha = if (mealEnabled) 1f else 0.4f
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showModal) {
        if (!isInSleepRange) {
            DayTimeModal(
                todayRecordDto = todayRecordDto,
                onDismissRequest = {
                    showModal = false
                },
                onNavigateToToday = {
                    // TODO: 콜백 호출 -> (데이터) 필요한 작업 수행
                    showModal = false
                },
                healthData = healthData,
            )
        }
        else {
            val formatDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            RecordedModal(
                onDismissRequest = { showModal = false },
                selectedDate = formatDate
            )
        }
    }
}
