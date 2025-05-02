package com.example.eggi.calendar.ui

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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.eggi.R
import com.example.eggi.calendar.controller.RecordedController
import com.example.eggi.calendar.data.model.DetailReportDto

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
                                val context = LocalContext.current
                                val imageLoader = remember {
                                    ImageLoader.Builder(context)
                                        .components {
                                            add(GifDecoder.Factory())
                                            add(ImageDecoderDecoder.Factory())
                                        }
                                        .build()
                                }

                                Box(modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(12.dp))
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(context)
                                                .data(report.bansoogiResource)
                                                .build(),
                                            imageLoader = imageLoader
                                        ),
                                        contentDescription = "반숙이",
                                        modifier = Modifier
                                            .offset(x = 20.dp, y = (-8).dp)
                                            .scale(1.5f)
                                            .size(100.dp)
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

                                    Spacer(modifier = Modifier.height(20.dp))

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

                        Spacer(modifier = Modifier.height(8.dp))

                        // 행동 기록 섹션
                        SectionHeader(
                            title = "행동 기록"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "누워있던 시간 :",
                            value = report.lyingTime,
                            unit = "분"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "앉아있던 시간 :",
                            value = report.sittingTime,
                            unit = "분"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "기상 이벤트 :",
                            value = report.standupCount,
                            unit = "회"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "스트레칭 이벤트 :",
                            value = report.stretchCount,
                            unit = "회"
                        )

                        // 이벤트 내역
                        // 로그 불러와서 적용해야 함
                        Row(modifier = Modifier
                            .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "10:57분",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                            )

                            Text(
                                text = "43분 앉아있음 추적",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            label = "휴대폰 미사용 이벤트 :",
                            value = report.phoneOffCount,
                            unit = "회"
                        )
                    }
                }
            }
        }
    }
}

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

@Preview(showBackground = true)
@Composable
fun RecordedModalPreview() {
//    RecordedModal(
//        onDismissRequest = { },
//        selectedDate = { }
//    )
}