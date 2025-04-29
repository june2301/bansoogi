package com.example.eggi.calendar.ui

import com.example.eggi.R
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.VerticalAlignmentLine
import java.nio.file.WatchEvent
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters.firstDayOfMonth

@Composable
fun CalendarScreen() {
    // 실제 오늘 날짜 가져오기
    val today = remember { LocalDate.now() }

    // 캘린더에서 확인하는 날짜
    var viewDate by remember { mutableStateOf(today)}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x90EEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 월 표시 및 화살표
            CalendarHeader(viewDate = viewDate)

            // 캘린더 표
            CalendarGrid(viewDate = viewDate, today = today)
        }
    }
}

@Composable
fun CalendarHeader(viewDate: LocalDate) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment  = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // 이전 달 선택 화살표
        IconButton(
            onClick = {
                Toast.makeText(context, "왼쪽", Toast.LENGTH_SHORT).show()
            }) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Prev"
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 선택한 월
        Text(
            text = "${viewDate.year}년 ${viewDate.monthValue}월",
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 다음 달 선택 화살표
        IconButton(onClick = {
            Toast.makeText(context, "오른쪽", Toast.LENGTH_SHORT).show()
        }) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Next"
            )
        }
    }
}

@Composable
fun CalendarGrid(viewDate: LocalDate, today: LocalDate) {
    Column (modifier = Modifier
        .fillMaxSize()
    ) {
        DaysHeader()

        Spacer(modifier = Modifier.height(12.dp))

        // 현재 보고 있는 월의 정보를 계산
        val yearMonth = YearMonth.of(viewDate.year, viewDate.monthValue)
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth().dayOfMonth

        // 1일의 요일
        val firstDayDayOfWeek = firstDayOfMonth.dayOfWeek.value

        // 일요일을 0으로 변환
        val emptyDaysAtStart = if (firstDayDayOfWeek == 7) 0 else firstDayDayOfWeek

        // 오늘 날짜인가?
        val currentDay = if (viewDate.year == today.year && viewDate.monthValue == today.monthValue)
            today.dayOfMonth else -1

        // 빈 셀과 날짜를 포함한 리스트 생성
        val calendarDays = List(emptyDaysAtStart) { 0 } + (1..lastDayOfMonth).toList()
        val chunks = calendarDays.chunked(7)

        // 날짜 표 출력
        Column(modifier = Modifier.weight(1f)) {
            chunks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // 각 주가 동일한 높이를 가지도록 설정
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEachIndexed { index, day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(), // 높이를 최대로 채움
                            contentAlignment = Alignment.Center
                        ) {
                            CalendarDayCell(
                                day = day,
                                isCurrentDay = day == currentDay,
                                dayOfWeek = index, // 0=일요일, 6=토요일
                            )
                        }
                    }

                    // 마지막 주가 7일 미만인 경우 빈 셀 추가
                    repeat(7 - week.size) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
fun DaysHeader() {
    // 요일 헤더
    val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")

    // 요일 표시
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        daysOfWeek.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun CalendarDayCell(day: Int, isCurrentDay: Boolean = false, dayOfWeek: Int = -1) {
    if (day > 0) { // 실제 날짜만 표시
        // 셀 클릭 시, 회색 창 제거
        val interactionSource = remember { MutableInteractionSource() }

        // 날짜 출력
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // 리플 효과(회색 창) 제거
                    onClick = { })
                .clip(RectangleShape)
                .let {
                    if (isCurrentDay) {
                        it.background(Color.Magenta) // 오늘 날짜는 배경색 추가
                    } else {
                        it
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 날짜 번호는 위쪽에 표시
            Text(
                text = day.toString(),
                fontSize = 20.sp,
                color = when (dayOfWeek) {
                    0 -> Color.Red  // 일요일
                    6 -> Color.Blue // 토요일
                    else -> Color.Black
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
            )

            // 이미지는 중앙에 배치
            // 현재는 무조건 보이지만, 나중에는 반숙이 데이터가 존재하는 경우에만 출력
            if (true) {
                val context = LocalContext.current
                val imageLoader = remember {
                    ImageLoader.Builder(context)
                        .components {
                            add(GifDecoder.Factory())
                            add(ImageDecoderDecoder.Factory())
                        }
                        .build()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.9f)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(R.drawable.bansoogi_walk)
                                .build(),
                            imageLoader = imageLoader
                        ),
                        contentDescription = "반숙이",
                        modifier = Modifier
                            .scale(1.5f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreView() {
    CalendarScreen()
}
