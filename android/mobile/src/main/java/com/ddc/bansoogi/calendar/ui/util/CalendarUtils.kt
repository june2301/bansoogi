package com.ddc.bansoogi.calendar.ui.util

import java.time.LocalDate
import java.time.YearMonth
import kotlin.collections.chunked

object CalendarUtils {
    private const val INITIAL_MONTH = 3000
    private const val INITIAL_PAGE = INITIAL_MONTH / 2

    fun getInitialPage(): Int = INITIAL_PAGE
    fun getInitialMonthCount(): Int = INITIAL_MONTH

    fun calculatePagerPage(initialDate: LocalDate, targetDate: LocalDate): Int {
        val yearDiff = targetDate.year - initialDate.year
        val monthDiff = targetDate.monthValue - initialDate.monthValue

        return INITIAL_PAGE + (yearDiff * 12 + monthDiff)
    }

    fun toFormattedDateString(date: LocalDate, day: Int): String {
        return "%04d-%02d-%02d".format(date.year, date.monthValue, day)
    }

    fun withDayOfMonth(date: LocalDate, day: Int): LocalDate {
        return LocalDate.of(date.year, date.monthValue, day)
    }

    fun isCurrentDay(viewDate: LocalDate, today: LocalDate): Int {
        return if (viewDate.year == today.year && viewDate.monthValue == today.monthValue)
            today.dayOfMonth
        else
            -1
    }

    fun getCalendarDays(yearMonth: YearMonth): List<Int> {
        // 현재 보고 있는 월의 정보를 계산
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth().dayOfMonth

        // 1일의 요일
        val firstDayDayOfWeek = firstDayOfMonth.dayOfWeek.value

        // 일요일을 0으로 변환
        val emptyDaysAtStart = if (firstDayDayOfWeek == 7) 0 else firstDayDayOfWeek

        val calendarDays = List(emptyDaysAtStart) { 0 } + (1..lastDayOfMonth).toList()

        return calendarDays
    }

    fun getWeekChunks(yearMonth: YearMonth): List<List<Int>> {
        return getCalendarDays(yearMonth).chunked(7)
    }
}