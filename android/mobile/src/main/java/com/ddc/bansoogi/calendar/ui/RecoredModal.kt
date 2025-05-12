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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
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
import com.ddc.bansoogi.common.ui.component.BansoogiAnimation
import com.ddc.bansoogi.main.ui.InfoRow
import com.ddc.bansoogi.main.ui.SectionHeader
import com.ddc.bansoogi.common.util.mapper.ActivityLogMapper.toKoreanBehaviorState
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
            onDismissRequest()
            return@LaunchedEffect
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
            painter = painterResource(id = R.drawable.egg_before_broken),
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
fun SectionHeader(
    title: String,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.DarkGray
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.egg_before_broken),
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
    value: Int,
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
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (highlightText != null) {
                Text(
                    text = highlightText,
                    fontSize = 14.sp,
                    color = highlightColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = value.toString(),
                fontSize = 16.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun Divider() {
    androidx.compose.material3.Divider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = Color.LightGray,
        thickness = 1.dp
    )
}

@Composable
fun VerticalSpacer(
    height: Dp = 8.dp
) {
    Spacer(modifier = Modifier.height(height))
}

@Composable
fun RecordContent(
    onDismissRequest: () -> Unit,
    report: DetailReportDto
) {
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
                .fillMaxHeight(0.7f)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {

                // 날짜 문자열에서 년, 월, 일 추출
                val (year, month, day) = report.date.split("-").map { it.toInt() }

                Box(modifier = Modifier.weight(0.1f)) {
                    ModalHeader(
                        title = "${month}월 ${day}일 행동 기록"
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()), // 넘쳤을 때, 스크롤
                        // 일정한 간격 설정
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 획득한 반숙이 정보
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 이미지 왼쪽 배치
                            Box(
                                modifier = Modifier
                                    .weight(1f) // 전체 너비의 1/2 차지
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(12.dp))
                                ) {
                                    BansoogiAnimation(
                                        resource = report.bansoogiResource,
                                        description = "행동 기록 모달에 출력하는 반숙이 리소스",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .offset(x = 20.dp, y = (-8).dp) // 중앙이 안 맞아서 조절
                                    )
                                }
                            }

                            // 텍스트를 오른쪽 절반에 배치하고 중앙 정렬
                            Box(
                                modifier = Modifier
                                    .weight(1f) // 전체 너비의 1/2 차지
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center // 내부 콘텐츠 중앙 정렬
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = buildAnnotatedString {
                                            append("에너지 : ")
                                            // 점수만 다른 색상으로 표기
                                            withStyle(
                                                style = SpanStyle(
                                                    color = Color(0xFF2E616A),  // 색상 변경
                                                )
                                            ) {
                                                append("${report.finalEnergyPoint}")
                                            }
                                            append(" / 100")
                                        },
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
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

                        VerticalSpacer()

                        // 행동 기록 섹션
                        SectionHeader(
                            title = "행동 기록"
                        )

                        VerticalSpacer()

                        InfoRow(
                            label = "누워있던 시간 :",
                            value = report.lyingTime,
                            unit = "분"
                        )

                        VerticalSpacer()

                        InfoRow(
                            label = "앉아있던 시간 :",
                            value = report.sittingTime,
                            unit = "분"
                        )

                        VerticalSpacer()

                        InfoRow(
                            label = "휴대폰 사용 시간 :",
                            value = report.phoneTime,
                            unit = "분"
                        )

                        Divider()

                        // 이벤트 행동 변화 섹션
                        SectionHeader(
                            title = "이벤트 행동 변화"
                        )

                        VerticalSpacer()

                        InfoRow(
                            label = "기상 이벤트 :",
                            value = report.standupCount,
                            unit = "회"
                        )

                        ActivityLogList(report.standLog)

                       VerticalSpacer()

                        InfoRow(
                            label = "스트레칭 이벤트 :",
                            value = report.stretchCount,
                            unit = "회"
                        )

                        ActivityLogList(report.stretchLog)

                        VerticalSpacer()

                        InfoRow(
                            label = "휴대폰 미사용 이벤트 :",
                            value = report.phoneOffCount,
                            unit = "회"
                        )

                        ActivityLogList(report.phoneOffLog)

                        // 건강 정보 섹션
                        SectionHeader(
                            title = "건강 정보"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "총 걸음수 :",
                            value = report.walkCount,
                            unit = " 회"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "총 계단 수 :",
                            value = report.stairsClimbed.toInt(),
                            unit = " 계단"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

//                        InfoRow(
//                            label = "수면 시간 :",
//                            value = report.sleepData.toInt(),
//                            unit = " 분"
//                        )
                    }
                }
            }
        }
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
            finalEnergyPoint =90,

            bansoogiTitle = "임시 반숙이",
            bansoogiResource = 1,

            standupCount = 1,
            standLog = emptyList(),

            stretchCount = 2,
            stretchLog = emptyList(),

            phoneOffCount = 3,
            phoneOffLog = emptyList(),

            lyingTime = 10,
            sittingTime = 20,
            phoneTime = 30,
            sleepTime = 40,

            walkCount = 100,
            runTime = 200,
            exerciseTime = 300,
            stairsClimbed = 400,

            breakfast = false,
            lunch = false,
            dinner = false
        )
    )
}