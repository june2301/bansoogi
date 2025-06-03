package com.ddc.bansoogi.calendar.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ddc.bansoogi.R
import com.ddc.bansoogi.calendar.controller.RecordedController
import com.ddc.bansoogi.calendar.data.model.DetailReportDto
import com.ddc.bansoogi.common.data.model.ActivityLogDto
import com.ddc.bansoogi.common.ui.component.SpriteSheetAnimation
import com.ddc.bansoogi.main.ui.InfoRow
import com.ddc.bansoogi.main.ui.SectionHeader
import com.ddc.bansoogi.common.util.mapper.ActivityLogMapper.toKoreanBehaviorState
import com.google.accompanist.pager.*
import kotlin.String

@Composable
fun RecordedModal(
    onDismissRequest: () -> Unit,
    selectedDate: String
) {
    val controller = remember { RecordedController() }
    var report by remember { mutableStateOf<DetailReportDto?>(null) }

    LaunchedEffect(selectedDate) {
        report = controller.getDetailReport(selectedDate)

        if (report == null) {
            onDismissRequest
        }
    }

    report?.let { report ->
        RecordContent(
            onDismissRequest = onDismissRequest,
            report = report
        )
    }
}

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
            painter = painterResource(id = R.drawable.egg_before_broken_gif),
            contentDescription = "아이콘",
            modifier = Modifier.size(32.dp)
        )

        VerticalSpacer()

        Text(
            text = title,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor
        )
    }
}

@Composable
fun VerticalSpacer(
    height: Dp = 8.dp
) {
    Spacer(modifier = Modifier.height(height))
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun RecordContent(
    onDismissRequest: () -> Unit,
    report: DetailReportDto
) {
    val context = LocalContext.current
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
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier) {
                // 1. 헤더 고정
                ModalHeader(title = "${report.date.split("-")[1]}월 ${report.date.split("-")[2]}일 행동 기록")

                VerticalSpacer(height = 16.dp)

                // 2. 에너지 및 반숙이 정보 고정
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFBD752))
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 이미지 왼쪽 배치
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xE6FFFFFF))
                            .border(3.dp, Color(0xFF725554), RoundedCornerShape(12.dp))
                        ) {
                            SpriteSheetAnimation(
                                context = context,
                                spriteSheetName = "${report.bansoogiGifUrl}_sheet.png",
                                jsonName = "${report.bansoogiImageUrl}.json",
                                modifier = Modifier
                                    .size(160.dp)
                                    .scale(1.3f)
                            )
                        }
                    }

                    // 텍스트를 오른쪽 절반에 배치하고 중앙 정렬
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        contentAlignment = Alignment.Center // 내부 콘텐츠 중앙 정렬
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(
                                            fontSize = 18.sp,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("에너지 : ")
                                    }

                                    withStyle(
                                        style = SpanStyle(
                                            fontSize = 20.sp,
                                            color = Color(0xFF2E616A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("${report.finalEnergyPoint}")
                                    }

                                    withStyle(
                                        style = SpanStyle(
                                            fontSize = 16.sp,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("/100")
                                    }
                                }
                            )

                            VerticalSpacer(height = 20.dp)

                            Text(
                                text = report.bansoogiTitle,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "획득!",
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        }
                    }
                }

                VerticalSpacer(height = 16.dp)

                // 3. Pager 내용
                HorizontalPager(
                    count = 3,
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) { page ->
                    when (page) {
                        0 -> PageBehaviorLogs(report)
                        1 -> PageEventLogs(report)
                        2 -> PageHealthInfo(report)
                    }
                }

                // 4. 인디케이터
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
fun PageBehaviorLogs(report: DetailReportDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Top
    ) {
        SectionHeader(title = "가만히 보낸 시간")
        VerticalSpacer(height = 16.dp)
        InfoRow(label = "누워있던 시간", value = report.lyingTime, unit = " 분")
        VerticalSpacer()
        InfoRow(label = "앉아있던 시간", value = report.sittingTime, unit = " 분")
        VerticalSpacer()
        InfoRow(label = "휴대폰 사용 시간", value = report.phoneTime, unit = " 분")
    }
}

@Composable
fun PageEventLogs(report: DetailReportDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Top
    ) {
        SectionHeader(title = "특별 보너스")
        VerticalSpacer(height = 16.dp)
        InfoRow(label = "기상", value = report.standupCount, unit = " 회")
//        ActivityLogList(report.standLog)
        VerticalSpacer()
        InfoRow(label = "스트레칭", value = report.stretchCount, unit = " 회")
        VerticalSpacer()
        InfoRow(label = "휴대폰 미사용", value = report.phoneOffCount, unit = " 회")
//        ActivityLogList(report.phoneOffLog)
    }
}

@Composable
fun PageHealthInfo(report: DetailReportDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Top
    ) {
        SectionHeader(title = "건강하게 보낸 시간")
        VerticalSpacer(height = 16.dp)
        InfoRow(label = "총 걸음 수", value = report.walkCount, unit = " 회")
        VerticalSpacer()
        InfoRow(label = "총 계단 수", value = report.stairsClimbed.toInt(), unit = " 계단")
        VerticalSpacer()
        InfoRow(label = "수면 시간", value = report.sleepTime, unit = " 분")
        VerticalSpacer()
        InfoRow(label = "운동 시간", value = report.exerciseTime, unit = " 분")
    }
}

@Composable
fun ActivityLogList(logs: List<ActivityLogDto>) {
    Column {
        logs.forEach { log ->
            ActivityLogItem(log)
        }
    }
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

@Preview(showBackground = true)
@Composable
fun RecordedModalPreview() {
    RecordContent(
        onDismissRequest = { },
        report = DetailReportDto (
            date = "2025-05-01",
            finalEnergyPoint = 90,

            bansoogiTitle = "임시 반숙이",
            bansoogiGifUrl = "1",
            bansoogiImageUrl = "1",

            standupCount = 1,
            standLog = emptyList(),

            stretchCount = 2,
            stretchLog = emptyList(),

            phoneOffCount = 3,
            phoneOffLog = emptyList(),

            lyingTime = 10,
            sittingTime = 20,
            phoneTime = 30,

            walkCount = 100,
            stairsClimbed = 200,
            sleepTime = 300,
            exerciseTime = 400,

            breakfast = false,
            lunch = false,
            dinner = false
        )
    )
}