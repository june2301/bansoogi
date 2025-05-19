package com.ddc.bansoogi.main.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.wear.tooling.preview.devices.WearDevices
import com.ddc.bansoogi.R
import com.ddc.bansoogi.common.data.enum.MealType
import com.ddc.bansoogi.common.mobile.communication.sender.MobileMyInfoSender
import com.ddc.bansoogi.common.mobile.communication.sender.MobileTodayRecordSender
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.ui.BackgroundImage
import com.ddc.bansoogi.common.ui.SpriteSheetAnimation
import com.ddc.bansoogi.common.ui.VerticalSpacer
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import com.ddc.bansoogi.main.ui.util.getConfig
import com.ddc.bansoogi.myinfo.data.dto.MyInfoDto
import com.ddc.bansoogi.myinfo.state.MyInfoStateHolder
import com.ddc.bansoogi.today.data.dto.ReportDto
import com.ddc.bansoogi.today.data.store.getCachedReport
import com.ddc.bansoogi.today.state.TodayRecordStateHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(navController: NavHostController) {
    val context = LocalContext.current

    // 초기 로컬 데이터 로딩
    LaunchedEffect(Unit) {
        // 초기에는 로컬에서 데이터를 호출
        val cached = getCachedReport(context).first()
        TodayRecordStateHolder.update(cached)

        // 모바일로 데이터 송신 요청을 전송
        MobileTodayRecordSender.sendEnergyRequest(context)
        MobileTodayRecordSender.send(context)
        MobileMyInfoSender.send(context)
    }

    val report = TodayRecordStateHolder.reportDto ?: ReportDto.default()
    val myInfo = MyInfoStateHolder.myInfoDto ?: MyInfoDto.default()

    val currentTimeState = remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            val nowMillis = System.currentTimeMillis()
            val delayMillis = 60_000L - (nowMillis % 60_000L)
            delay(delayMillis)
            currentTimeState.value = LocalTime.now()
        }
    }

    val currentTime = currentTimeState.value

    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val mealTimesWithType = remember(myInfo) {
        listOfNotNull(
            myInfo.breakfastTime.takeIf { it.isNotBlank() }?.let {
                MealType.BREAKFAST to LocalTime.parse(it, fmt)
            },
            myInfo.lunchTime.takeIf { it.isNotBlank() }?.let {
                MealType.LUNCH to LocalTime.parse(it, fmt)
            },
            myInfo.dinnerTime.takeIf { it.isNotBlank() }?.let {
                MealType.DINNER to LocalTime.parse(it, fmt)
            }
        )
    }

    val currentMealTypes by remember(currentTime, mealTimesWithType) {
        derivedStateOf {
            mealTimesWithType.filter { (_, t) ->
                val start = t.minusMinutes(30)
                val end   = t.plusMinutes(30)
                !currentTime.isBefore(start) && !currentTime.isAfter(end)
            }.map { it.first }
        }
    }

    val pendingMealTypes by remember(currentMealTypes, report) {
        derivedStateOf {
            currentMealTypes.filter { type ->
                when (type) {
                    MealType.BREAKFAST -> report.breakfast == false
                    MealType.LUNCH     -> report.lunch     == false
                    MealType.DINNER    -> report.dinner    == false
                }
            }
        }
    }

    val isMealEnabled = pendingMealTypes.isNotEmpty()
    val currentMealType = pendingMealTypes
        .sortedBy { listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER).indexOf(it) }
        .firstOrNull()

    var progressValue = TodayRecordStateHolder.reportDto?.energyPoint ?: 0

    // 상호작용 상태 변수
    var triggerInteraction by remember { mutableStateOf(false) }

    // 식사 애니메이션
    var triggerMeal by remember { mutableStateOf(false) }

    // 임시 호출
//    LaunchedEffect(triggerInteraction) {
//        if (triggerInteraction) {
//            // 5초 후에 showInteraction을 false로 설정하고 onFinished 콜백 호출
//            delay(5000)
//            triggerInteraction = false
//        }
//    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        BackgroundImage()

        SideButtons(
            navController = navController,
//            hasMeal       = isMealEnabled,
            hasMeal = true,
            onMealClick   = {
                // 1. 반숙이 식사 움직임 출력 -> 변수 변경
                triggerMeal = true

                // 2. 식사 데이터 처리
                currentMealType?.let { type ->
                    MobileTodayRecordSender.sendMealTrigger(context, type.name)
                }
            },
            onInteractionBtnClick = {
                // 1. 반숙이 상호작용 움직임 출력 -> 변수 변경
                triggerInteraction = true

                // 2. 모바일로 상호작용 전송
                MobileTodayRecordSender.sendInteractionTrigger(context)
            }
        )

        BansoogiContent(
            progressValue = progressValue,
            triggerInteraction = triggerInteraction,
            triggerMeal = triggerMeal,
            onInteractionEnd = {
                triggerInteraction = false
            },
            onMealEnd = {
                triggerMeal = false
            }
        )
    }
}

@Composable
fun SideButtons(
    navController: NavHostController,
    hasMeal: Boolean,
    onMealClick: () -> Unit,
    onInteractionBtnClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column() {
            IconCircleButton(
                iconResource = R.drawable.cookie,
                size = 20.dp,
                description = "식사 버튼",
                enabled      = hasMeal,
                onBtnClick   = onMealClick
            )

            VerticalSpacer()

            IconCircleButton(
                iconResource = R.drawable.bansoogi_face,
                size = 32.dp,
                description = "상호 작용"
            ) {
                onInteractionBtnClick()
            }
        }

        IconCircleButton(
            iconResource = R.drawable.dehaze,
            size = 20.dp,
            description = "메뉴 버튼"
        ) {
            navController.navigate(NavRoutes.MENU)
        }
    }
}

@Composable
fun IconCircleButton(
    iconResource: Int,
    size: Dp,
    description: String,
    enabled: Boolean = true,
    onBtnClick: () -> Unit) {
    // 셀 클릭 시, 회색 창 제거
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(36.dp)
            .height(36.dp)
            .background(
                color = if (enabled) Color.White else Color.LightGray,
                shape = RoundedCornerShape(50)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null, // 리플 효과(회색 창) 제거
                onClick = onBtnClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconResource),
            contentDescription = description,
            modifier = Modifier
                .width(size)
                .height(size),
            contentScale = ContentScale.Fit,
            alpha = if (enabled) 1f else 0.4f
        )
    }
}

@Composable
fun BansoogiContent(
    progressValue: Int,
    triggerInteraction: Boolean,
    triggerMeal: Boolean,
    onInteractionEnd: () -> Unit,
    onMealEnd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BansoogiAnimationContent(
            triggerInteraction = triggerInteraction,
            triggerMeal = triggerMeal,
            onInteractionEnd = onInteractionEnd,
            onMealEnd = onMealEnd
        )

        EnergyBar(
            progressValue = progressValue
        )
    }
}

@Composable
fun BansoogiAnimationContent(
    triggerInteraction: Boolean,
    triggerMeal: Boolean,
    onInteractionEnd: () -> Unit,
    onMealEnd: () -> Unit
) {
    if (triggerInteraction) {
        BansoogiAnimation(
            state = BansoogiState.SMILE,
            loop = false,
            loopCouont = 3,
            onAnimationEnd = onInteractionEnd
        )
    } else if (triggerMeal) {
        BansoogiAnimation(
            state = BansoogiState.EAT,
            loop = false,
            loopCouont = 1,
            onAnimationEnd = onMealEnd
        )
    } else {
        BansoogiAnimation(
            state = BansoogiStateHolder.state
        )
    }
}

@Composable
fun BansoogiAnimation(
    state: BansoogiState,
    loop: Boolean = true,
    loopCouont: Int = 0,
    onAnimationEnd: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var config = state.getConfig()

    SpriteSheetAnimation(
        context = context,
        spriteSheetName = "${config.sprite}_sheet.png",
        jsonName = "${config.json}.json",
        loop = loop,
        loopCount = loopCouont,
        onAnimationEnd = onAnimationEnd,
        modifier = Modifier
            .fillMaxSize(0.8f)
    )
}

@Composable
fun EnergyBar(progressValue: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .offset(y = (-8).dp)
            .height(12.dp)
            .background(
                color = Color.White,
            )
            .border(
                width = 2.dp,
                color = Color.DarkGray,
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progressValue / 100f)
                .fillMaxHeight()
                .background(Color.Green)
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MainScreen(rememberNavController())
}