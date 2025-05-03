package com.ddc.bansoogi.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import com.ddc.bansoogi.calendar.controller.CalendarController
import com.ddc.bansoogi.calendar.data.model.HistoryItemDto
import com.ddc.bansoogi.calendar.view.CalendarView
import com.ddc.bansoogi.common.ui.component.BansoogiAnimation
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

// 미리 로드할 월의 수 -> 무한 스크롤
private const val INITIAL_PAGE = Int.MAX_VALUE / 2

@Composable
fun CalendarScreen() {
    // 캘린더 데이터 가져오기 -> 임시로 데이터 초기화, 없다면 생성함
    val historyState = remember { mutableStateOf<List<HistoryItemDto>?>(null) }
    val view = remember {
        object : CalendarView {
            override fun displayCalendar(history: List<HistoryItemDto>) {
                historyState.value = history
            }
        }
    }

    val controller = remember { CalendarController(view) }
    LaunchedEffect(Unit) {
        controller.initialize()
    }

    historyState.value?.let { history ->
        CalendarContent(history = history)
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("로딩 중...", fontSize = 16.sp)
    }
}

@Composable
fun CalendarContent(history: List<HistoryItemDto>) {
    // 실제 오늘 날짜 가져오기
    val today = remember { LocalDate.now() }

    // 초기 날짜와 현재 보여지는 날짜
    val initialDate = remember { today }
    var currentViewDate by remember { mutableStateOf(initialDate) }

    // 모달 표시 상태 및 선택한 날짜
    var showModal by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<String?>(null) }

    // 페이저 상태 생성
    val pagerState = rememberPagerState(
        pageCount = { Int.MAX_VALUE },
        initialPage = INITIAL_PAGE
    )

    // 코루틴 스코프 생성
    val coroutineScope = rememberCoroutineScope()

    // 페이지 변경 시 날짜 업데이트
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val monthDiff = page - INITIAL_PAGE
            currentViewDate = initialDate.plusMonths(monthDiff.toLong())
        }
    }

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
            CalendarHeader(
                viewDate = currentViewDate,
                onPrevMonth = {
                    // 코루틴 스코프 내에서 애니메이션 실행
                    coroutineScope.launch {
                        if (pagerState.canScrollBackward) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onNextMonth = {
                    // 코루틴 스코프 내에서 애니메이션 실행
                    coroutineScope.launch {
                        if (pagerState.canScrollForward) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )

            // HorizontalPager 사용
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fill,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val monthDiff = page - INITIAL_PAGE
                val pageDate = initialDate.plusMonths(monthDiff.toLong())

                // 캘린더 표
                CalendarGrid(
                    viewDate = pageDate,
                    today = today,
                    history = history,
                    onDayClick = { day ->
                        selectedDate = "%04d-%02d-%02d".format(pageDate.year, pageDate.monthValue, day)
                        showModal = true
                    }
                )
            }
        }
    }

    // 모달 띄우기
    if (showModal && selectedDate != null) {
        RecordedModal(
            onDismissRequest = { showModal = false },
            selectedDate = selectedDate!!
        )
    }
}

@Composable
fun CalendarHeader(viewDate: LocalDate, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
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
                onPrevMonth()
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
            onNextMonth()
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
fun CalendarGrid(viewDate: LocalDate, today: LocalDate, history: List<HistoryItemDto>, onDayClick: (Int) -> Unit) {
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
                            // day가 0보다 클 때만
                            if (day > 0) {
                                val cellDate = LocalDate.of(viewDate.year, viewDate.monthValue, day)
                                val matchedHistory = history.find { it.date == cellDate }

                                CalendarDayCell(
                                    day = day,
                                    isCurrentDay = day == currentDay,
                                    dayOfWeek = index, // 0=일요일, 6=토요일
                                    bansoogiResource = matchedHistory?.bansoogiAnimationId,
                                    onCellClick = { onDayClick(day) }
                                )
                            }
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
fun CalendarDayCell(day: Int, isCurrentDay: Boolean = false, dayOfWeek: Int = -1, bansoogiResource: Int?, onCellClick: () -> Unit) {
        // 셀 클릭 시, 회색 창 제거
        val interactionSource = remember { MutableInteractionSource() }

        // 날짜 색
        val dayColor = if (isCurrentDay) {
            Color.White // 오늘 날자
        } else {
            when(dayOfWeek) {
                0 -> Color.Red  // 일요일
                6 -> Color.Blue // 토요일
                else -> Color.Black
            }
        }

        // 날짜 출력
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // 리플 효과(회색 창) 제거
                    onClick = onCellClick)
                .clip(RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            // 오늘 날짜일 경우 원형 배경 추가
            if (isCurrentDay) {
                Box(
                    modifier = Modifier
                        .size(36.dp) // 원의 크기 설정
                        .background(
                            Color.Red, // 원의 배경색
                            shape = CircleShape // 색 모양
                        )
                        .align(Alignment.TopCenter)
                )
            }

            // 날짜 번호는 위쪽에 표시
            Text(
                text = day.toString(),
                fontSize = 20.sp,
                color = dayColor,
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.TopCenter)
            )

            // 이미지는 중앙에 배치
            // 현재는 무조건 보이지만, 나중에는 반숙이 데이터가 존재하는 경우에만 출력s
            if (bansoogiResource != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.9f)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    BansoogiAnimation(
                        resource = bansoogiResource,
                        description = "달력에 표시하는 반숙이 리소스",
                        modifier = Modifier
                    )
                }
            }
        }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreView() {
    CalendarScreen()
}
