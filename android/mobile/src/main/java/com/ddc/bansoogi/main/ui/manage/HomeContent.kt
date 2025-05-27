package com.ddc.bansoogi.main.ui.manage

import android.R.attr.maxWidth
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ddc.bansoogi.R
import com.ddc.bansoogi.calendar.ui.RecordedModal
import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.common.data.enum.MealType
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.main.controller.TodayRecordController
import com.ddc.bansoogi.main.ui.DayTimeModal
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import com.ddc.bansoogi.main.ui.util.InteractionUtil
import com.ddc.bansoogi.myInfo.controller.MyInfoController
import com.ddc.bansoogi.nearby.NearbyConnectionManager
import com.ddc.bansoogi.nearby.data.BansoogiFriend
import com.ddc.bansoogi.nearby.ui.FriendFoundNotification
import com.ddc.bansoogi.nearby.ui.NearbyFloatingButton
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import kotlin.math.round

@Composable
fun HomeContent(
    todayRecordDto: TodayRecordDto,
    todayRecordController: TodayRecordController,
    isInSleepRange: Boolean,
    healthData: CustomHealthData,
    isSearching: Boolean,
    toggleNearby: () -> Unit,
    peers: List<com.ddc.bansoogi.nearby.data.BansoogiFriend>,
    nearbyMgr: NearbyConnectionManager,
    userNickname: String,
    showFriendBanner: Boolean = false,
    friendName: String = "",
    onDismissFriendBanner: () -> Unit = {},
    groupOffsetFraction: Float = 0.4f
) {
    // 1) HomeContent 최상단에 Box 추가
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        val parentHeight = this.maxHeight
        val yOffset = parentHeight * groupOffsetFraction

        // 2) Box 안 최상단에 알림 호출
        if (showFriendBanner) {
            FriendFoundNotification(
                friendName = friendName,
                isVisible = showFriendBanner,
                onDismiss = onDismissFriendBanner,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .zIndex(10f)      // zIndex 높여서 최상단에 렌더링
            )
        }

        val context = LocalContext.current

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

        val characterListState = remember { mutableStateOf<List<Character?>>(emptyList()) }
        LaunchedEffect(Unit) {
            characterListState.value = todayRecordController.getCurrentWeekDetailReports()
        }

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val mealTimesWithType = remember(myInfo) {
            listOfNotNull(
                myInfo?.breakfastTime?.let { MealType.BREAKFAST to it },
                myInfo?.lunchTime?.let { MealType.LUNCH to it },
                myInfo?.dinnerTime?.let { MealType.DINNER to it }
            ).mapNotNull { (type, str) ->
                runCatching { LocalTime.parse(str, formatter) }
                    .getOrNull()?.let { time -> type to time }
            }
        }

        val currentMealTypes by remember(currentTime.value, mealTimesWithType) {
            derivedStateOf {
                mealTimesWithType.filter { (_, time) ->
                    val start = time.minusMinutes(30)
                    val end = time.plusMinutes(30)
                    !currentTime.value.isBefore(start) && !currentTime.value.isAfter(end)
                }.map { it.first }
            }
        }

        val pendingMealTypes by remember(currentMealTypes, todayRecordDto) {
            derivedStateOf {
                currentMealTypes.filter { type ->
                    when (type) {
                        MealType.BREAKFAST -> !todayRecordDto.breakfast
                        MealType.LUNCH -> !todayRecordDto.lunch
                        MealType.DINNER -> !todayRecordDto.dinner
                    }
                }
            }
        }

        val mealEnabled = pendingMealTypes.isNotEmpty()

        // 상호 작용 애니메이션
        var triggerInteraction by remember { mutableStateOf(false) }

        // 식사 애니메이션
        var triggerMeal by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 4.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val today = remember { LocalDate.now() }
                val year = today.year.toString()
                val month = today.monthValue.toString()
                val day = today.dayOfMonth.toString()

                val dateText = buildAnnotatedString {
                    append(year)
                    withStyle(style = SpanStyle(fontSize = 18.sp)) { append(" 년  ") }

                    append(month)
                    withStyle(style = SpanStyle(fontSize = 18.sp)) { append(" 월  ") }

                    append(day)
                    withStyle(style = SpanStyle(fontSize = 18.sp)) { append(" 일") }
                }

                val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dateText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        characterListState.value.forEachIndexed { index, character ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = daysOfWeek.get(index),
                                    textAlign = TextAlign.Center,
                                    fontSize = 16.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (character == null) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.LightGray)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0x50FFD966))
                                    ) {
                                        val imageLoader = remember {
                                            ImageLoader.Builder(context)
                                                .components {
                                                    add(GifDecoder.Factory())
                                                    add(ImageDecoderDecoder.Factory())
                                                }
                                                .build()
                                        }

                                        val resId = context.resources.getIdentifier(
                                            character.imageUrl,
                                            "drawable",
                                            context.packageName
                                        )

                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                ImageRequest.Builder(context)
                                                    .data(resId)
                                                    .build(),
                                                imageLoader = imageLoader
                                            ),
                                            contentDescription = "반숙이",
                                            modifier = Modifier
                                                .scale(1.2f)
                                                .fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val randomMessages = remember(todayRecordDto, healthData) {
                        listOfNotNull(
                            todayRecordDto.lyingTime?.let { "오늘은 ${it}분 동안 누워 있었네!" },
                            todayRecordDto.sittingTime?.let { "오늘은 ${it}분 동안 앉아 있었네!" },
                            todayRecordDto.phoneTime?.let { "오늘은 휴대폰을 ${it}분 봤네!" },
                            healthData.step.toInt().let { "오늘은 ${it}보를 걸었어" },
                            healthData.floorsClimbed.toInt().let { "오늘은 ${it}계단을 올랐구나!" },
                            healthData.sleepData?.let { "오늘은 ${it}분 동안 잤구나!" },
                            healthData.exerciseTime?.let { "오늘은 ${it}분 동안 운동했어!" }
                        )
                    }
                    val selectedMessage = remember(randomMessages) {
                        if (randomMessages.isNotEmpty()) randomMessages.random() else "아직 데이터가 없어"
                    }

                    Text(
                        text = selectedMessage,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E616A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(
                        width = 2.dp,
                        color = Color.DarkGray,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Color(0xFFEEEEEE))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(todayRecordDto.energyPoint / 100f)
                        .height(32.dp)
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
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                ) {
                    when {
                        triggerInteraction -> {
                            BansoogiAnimation(
                                state = BansoogiState.SMILE,
                                loop = false,
                                loopCouont = 3,
                                onAnimationEnd = { triggerInteraction = false }
                            )
                        }
                        triggerMeal -> {
                            BansoogiAnimation(
                                state = BansoogiState.EAT,
                                loop = false,
                                loopCouont = 1,
                                onAnimationEnd = { triggerMeal = false }
                            )
                        }
                        else -> {
                            BansoogiAnimation(
                                state = BansoogiStateHolder.state
                            )
                        }
                    }
                }
            }

            val buttonShape = RoundedCornerShape(30.dp)
            val isCoolDown = remember { mutableStateOf(true) }

            LaunchedEffect(todayRecordDto.interactionLatestTime) {
                val remainingTime =
                    InteractionUtil.getRemainingCooldownMillis(todayRecordDto.interactionLatestTime)
                if (remainingTime > 0) {
                    isCoolDown.value = true
                    delay(remainingTime)
                }
                isCoolDown.value = false
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
//                .padding(horizontal = 64.dp),
                horizontalArrangement = Arrangement.Center,
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
                            tint = if (isCoolDown.value || isInSleepRange) Color.Gray else Color(
                                0xFF4CAF50
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

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
            Spacer(modifier = Modifier.height(32.dp))
        }

        Box(
            modifier = Modifier
                .offset(y = yOffset)
                .align(Alignment.TopEnd)
                .wrapContentSize()
                .zIndex(5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 12.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // ───────── Today 버튼 ─────────
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
                            ImageRequest.Builder(context).data(R.drawable.ic_today).build(),
                            imageLoader = imageLoader
                        ),
                        contentDescription = "Today",
                        modifier = Modifier
                            .size(55.dp)
                            .clickable { showModal = true },
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Today",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // ───────── Nearby 토글 ─────────
                NearbyFloatingButton(
                    isSearching = isSearching,
                    onClick = { toggleNearby() }
                )

                // Floating 버튼 대신
                if (peers.isNotEmpty()) {
                    VerticalFriendList(
                        peers = peers,
                        nearbyMgr = nearbyMgr,
                        userNickname = userNickname,
                        modifier = Modifier
                            .align(Alignment.End)
                    )
                }
            }
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
            } else {
                val formatDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                RecordedModal(
                    onDismissRequest = { showModal = false },
                    selectedDate = formatDate
                )
            }
        }
    }
}

//@OptIn(ExperimentalLayoutApi::class)
//@Composable
//private fun FriendList(
//    peers: List<BansoogiFriend>,
//    nearbyMgr: NearbyConnectionManager,
//    userNickname: String,
//    modifier: Modifier = Modifier
//) {
//    FlowRow(
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        verticalArrangement   = Arrangement.spacedBy(4.dp),
//        modifier = modifier
//    ) {
//        peers.take(5).forEach { p ->
//            Card(
//                modifier = Modifier
//                    .wrapContentWidth()
//                    .clickable { nearbyMgr.sendStaticWarnTo(p.endpointId, "SITTING_LONG", userNickname) }
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
//                ) {
//                    Text(
//                        text = "🥚 ${p.nickname}",
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 14.sp
//                    )
//                    p.distanceRssi?.let {
//                        Spacer(Modifier.width(4.dp))
//                        Text(
//                            text = "(${it}dBm)",
//                            style = MaterialTheme.typography.bodySmall
//                        )
//                    }
//                }
//            }
//        }
//        if (peers.size > 5) {
//            Card(modifier = Modifier.wrapContentWidth()) {
//                Text(
//                    text = "... 외 ${peers.size - 5}명",
//                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
//                    style = MaterialTheme.typography.bodySmall
//                )
//            }
//        }
//    }
//}

// HomeContent 안의 호출부는 그대로 두시고, Composable만 교체합니다.

@Composable
private fun VerticalFriendList(
    peers: List<BansoogiFriend>,
    nearbyMgr: NearbyConnectionManager,
    userNickname: String,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        // 최대 5명까지만
        peers.take(5).forEach { p ->
            Card(
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable {
                        nearbyMgr.sendStaticWarnTo(
                            p.endpointId,
                            "SITTING_LONG",
                            userNickname
                        )
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🥚 ${p.nickname}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    p.distanceRssi?.let {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "(${it}dBm)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        // 나머지 개수 표시
        if (peers.size > 5) {
            Card(modifier = Modifier.wrapContentWidth()) {
                Text(
                    text = "... 외 ${peers.size - 5}명",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
