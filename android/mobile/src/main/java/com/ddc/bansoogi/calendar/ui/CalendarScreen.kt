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
import com.ddc.bansoogi.calendar.data.model.CalendarMarkerDto
import com.ddc.bansoogi.calendar.ui.component.YearMonthPickerDialog
import com.ddc.bansoogi.calendar.ui.state.CalendarContentUiState
import com.ddc.bansoogi.calendar.ui.state.CalendarUiState
import com.ddc.bansoogi.calendar.ui.util.CalendarUtils
import com.ddc.bansoogi.calendar.view.CalendarView
import com.ddc.bansoogi.common.ui.component.BansoogiAnimation
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun loadCalendarData(): CalendarUiState {
    // 초기 데이터 상태 기억
    val state = remember { mutableStateOf(CalendarUiState()) }

    // 컨트롤러 + 뷰 연결
    val view = object : CalendarView {
        override fun displayCalendar(markers: List<CalendarMarkerDto>) {
            // 데이터를 받으면 상태를 갱신
            state.value = state.value.copy(
                isLoading = false,
                calendarMarkers = markers
            )
        }
    }
    val controller = remember { CalendarController(view) }

    // 화면 시작 시, 데이터 호출
    LaunchedEffect(Unit) {
        controller.initialize()
    }

    // 현재 상태를 화면으로 보여줌
    return state.value
}

@Composable
fun CalendarScreen() {
    val calendarState = loadCalendarData()

    if (calendarState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("로딩 중...", fontSize = 16.sp)
        }
    } else {
        CalendarContent(
            today = calendarState.today,
            calendarMarkers = calendarState.calendarMarkers
        )
    }
}

@Composable
fun CalendarContent(
    today: LocalDate,
    calendarMarkers: List<CalendarMarkerDto>
) {
    val initialDate = remember { today }
    var uiState by remember { mutableStateOf(CalendarContentUiState(currentViewDate = initialDate)) }

    // 페이저 상태 생성
    val pagerState = rememberPagerState(
        pageCount = { CalendarUtils.getInitialMonthCount() },
        initialPage = CalendarUtils.getInitialPage()
    )

    // 코루틴 스코프 생성
    val coroutineScope = rememberCoroutineScope()

    // 페이지 변경 시 날짜 업데이트
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val monthDiff = page - CalendarUtils.getInitialPage()
            uiState = uiState.copy(
                currentViewDate = initialDate.plusMonths(monthDiff.toLong())
            )
        }
    }

    // 특정 날짜로 페이저 이동 함수
    fun navigateToDate(targetDate: LocalDate) {
        val newPage = CalendarUtils.calculatePagerPage(initialDate, targetDate)

        // 페이저 이동
        coroutineScope.launch {
            pagerState.animateScrollToPage(newPage)
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
                viewDate = uiState.currentViewDate,
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
                },
                // 연월 피커 다이얼로그에서 날짜 선택 시 처리
                onDateSelect = { newDate ->
                    navigateToDate(newDate)
                }
            )

            // HorizontalPager 사용
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fill,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val monthDiff = page - CalendarUtils.getInitialPage()
                val pageDate = initialDate.plusMonths(monthDiff.toLong())

                // 캘린더 표
                CalendarGrid(
                    viewDate = pageDate,
                    today = today,
                    calendarMarkers = calendarMarkers,
                    onDayClick = { day ->
                        uiState = uiState.copy(
                            selectedDate = CalendarUtils.toFormattedDateString(pageDate, day),
                            showModal = true
                        )
                    }
                )
            }
        }
    }

    // 모달 띄우기
    if (uiState.showModal && uiState.selectedDate != null) {
        RecordedModal(
            onDismissRequest = {
                uiState = uiState.copy(showModal = false)
            },
            selectedDate = uiState.selectedDate!!
        )
    }
}

@Composable
fun CalendarHeader(
    viewDate: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelect: (LocalDate) -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }

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
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable {
                // 클릭 시 다이얼로그 표시
                showDialog = true
            }
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

    // 다이얼로그 표시
    if (showDialog) {
        YearMonthPickerDialog(
            currentDate = viewDate,
            onDateSelected = { year, month ->
                // 사용자가 선택한 연월로 날짜 업데이트
                val newDate = viewDate.withYear(year).withMonth(month)
                onDateSelect(newDate)
            },
            onDismiss = {
                // 다이얼로그 닫기
                showDialog = false
            }
        )
    }
}

@Composable
fun CalendarGrid(viewDate: LocalDate, today: LocalDate, calendarMarkers: List<CalendarMarkerDto>, onDayClick: (Int) -> Unit) {
    Column (modifier = Modifier
        .fillMaxSize()
    ) {
        DaysHeader()

        Spacer(modifier = Modifier.height(12.dp))

        // 오늘 날짜인가?
        val currentDay = CalendarUtils.isCurrentDay(viewDate, today)

        // 빈 셀과 날짜를 포함한 리스트 생성
        val chunks = CalendarUtils.getWeekChunks(YearMonth.of(viewDate.year, viewDate.monthValue))

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
                                val cellDate = CalendarUtils.withDayOfMonth(viewDate, day);
                                val matchedHistory = calendarMarkers.find { it.date == cellDate }

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
