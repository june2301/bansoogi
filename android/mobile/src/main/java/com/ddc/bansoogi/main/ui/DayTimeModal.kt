package com.ddc.bansoogi.main.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ddc.bansoogi.R
import com.ddc.bansoogi.common.data.model.ActivityLogDto
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.common.util.mapper.ActivityLogMapper.toKoreanBehaviorState
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlin.collections.forEach

@Composable
fun ModalHeader(
    title: String,
    fontSize: TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color(0xFF2E616A)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_today),
            contentDescription = "아이콘",
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    fontSize: TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.DarkGray
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.egg_before_broken_gif),
            contentDescription = "달걀",
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = fontSize,
            color = textColor,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: Int?,
    unit: String,
    modifier: Modifier = Modifier,
    highlightText: String? = null,
    highlightColor: Color = Color(0xFF4CAF50)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (highlightText != null) {
                Text(
                    text = highlightText,
                    fontSize = 16.sp,
                    color = highlightColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = value?.toString() ?: "",
                fontSize = 18.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value?.let { "${unit.toString()} " }?: "아직 계산 중이야",
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun VerticalSpacer(
    height: Dp = 8.dp
) {
    Spacer(modifier = Modifier.height(height))
}

@Composable
fun ActivityLogItem(log: ActivityLogDto) {
    Row(modifier = Modifier
        .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = log.reactedTime,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, start = 8.dp)
        )

        val readableState = log.fromState.toKoreanBehaviorState()
        val durationText = "${log.duration ?: 0}분 $readableState 추적"

        Text(
            text = durationText,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, start = 8.dp)
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun DayTimeModal(
    todayRecordDto: TodayRecordDto,
    onDismissRequest: () -> Unit,
    onNavigateToToday: () -> Unit,
    healthData: CustomHealthData,
) {
    val pagerState = rememberPagerState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier) {
                // 1. 헤더 고정
                ModalHeader(title = "오늘 기록")
                VerticalSpacer(height = 16.dp)

                // 2. 반숙이 그림
                val backgroundImage = when (pagerState.currentPage) {
                    0, 1 -> R.drawable.kitchen_flat
                    2 -> R.drawable.park_flat
                    else -> R.drawable.kitchen_flat
                }

                val characterImage = when (pagerState.currentPage) {
                    0 -> R.drawable.bansoogi_happy_sit
                    1 -> R.drawable.bansoogi_stand
                    2 -> R.drawable.bansoogi_walk_stop
                    else -> R.drawable.bansoogi_happy_sit
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = backgroundImage),
                        contentDescription = "배경",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Image(
                        painter = painterResource(id = characterImage),
                        contentDescription = "반숙이 이미지",
                        modifier = Modifier
                            .size(140.dp)
                            .align(Alignment.TopCenter)
                    )
                }

                VerticalSpacer(height = 16.dp)

                HorizontalPager(
                    count = 3,
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) { page ->
                    when (page) {
                        0 -> PageBehaviorLogs(todayRecordDto)
                        1 -> PageEventLogs(todayRecordDto)
                        2 -> PageHealthInfo(healthData)
                    }
                }

                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp),
                    activeColor = Color(0xFFEEC530),
                    inactiveColor = Color(0xFFD9D9D9)
                )
            }
        }
    }
}

@Composable
fun PageBehaviorLogs(todayRecordDto: TodayRecordDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Top
    ) {
        SectionHeader(title = "가만히 보낸 시간")
        VerticalSpacer(height = 16.dp)
        InfoRow("누워있던 시간", todayRecordDto.lyingTime, "분")
        VerticalSpacer()
        InfoRow("앉아있던 시간", todayRecordDto.sittingTime, "분")
        VerticalSpacer()
        InfoRow("휴대폰 사용 시간", todayRecordDto.phoneTime, "분")
    }
}

@Composable
fun PageEventLogs(todayRecordDto: TodayRecordDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Top
    ) {
        SectionHeader(title = "특별 보너스")
        VerticalSpacer(height = 16.dp)
        InfoRow("기상", todayRecordDto.standUpCnt, "회")
//        todayRecordDto.standLog.forEach { ActivityLogItem(it) }
        VerticalSpacer()
        InfoRow("스트레칭", todayRecordDto.stretchCnt, "회")
        VerticalSpacer()
        InfoRow("휴대폰 미사용", todayRecordDto.phoneOffCnt, "회")
//        todayRecordDto.phoneOffLog.forEach { ActivityLogItem(it) }
    }
}

@Composable
fun PageHealthInfo(healthData: CustomHealthData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Top
    ) {
        SectionHeader(title = "건강하게 보낸 시간")
        VerticalSpacer(height = 16.dp)
        InfoRow("총 걸음 수", healthData.step.toInt(), "회")
        VerticalSpacer()
        InfoRow("총 계단 수", healthData.floorsClimbed.toInt(), "계단")
        VerticalSpacer()
        InfoRow("수면 시간", healthData.sleepData, "분")
        VerticalSpacer()
        InfoRow("운동 시간", healthData.exerciseTime, "분")
    }
}
