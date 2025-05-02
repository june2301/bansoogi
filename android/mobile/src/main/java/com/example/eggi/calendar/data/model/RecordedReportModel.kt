package com.example.eggi.calendar.data.model

import com.example.eggi.calendar.data.local.RecordedReportDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RecordedReportModel {
    private val dataSource = RecordedReportDataSource()

    // 더미데이터용 추후 삭제 예정
    suspend fun initialize() {
        dataSource.initialize()
    }

    fun getReportHistoryItems(): Flow<List<HistoryItem>> =
        dataSource.getRecordedReportList().map { reportList ->
            reportList.map { report ->
                // 나중에 반숙이 클래스에 대한 data 함수들이 만들어진다면 그 함수들로 변경할 예정
                val bansoogi = dataSource.getBansoogiById(report.bansoogiId)

                HistoryItem(
                    date = LocalDate.parse(report.reportedDate),
                    bansoogiAnimationId =  bansoogi?.gifUrl
                )
            }
        }

    suspend fun getDetailReport(date: String): DetailReport? {
        val report = dataSource.getRecordedReportByDate(date) ?: return null

        // 반숙이 데이터 호출도 나중에 변경 예정
        val bansoogi = dataSource.getBansoogiById(report.bansoogiId)

        return DetailReport(
            date = report.reportedDate,

            finalEnergyPoint = report.finalEnergyPoint,

            bansoogiTitle = bansoogi?.title ?: "",
            bansoogiResource = bansoogi?.gifUrl ?: 0,

            standupCount = report.standupCount,
            stretchCount = report.stretchCount,
            phoneOffCount = report.phoneOffCount,

            lyingTime = report.lyingTime,
            sittingTime = report.sittingTime,
            phoneTime = report.phoneTime,
            sleepTime = report.sleepTime,

            walkCount = report.walkCount,
            runTime = report.runTime,
            exerciseTime =  report.exerciseTime,
            stairsClimbed = report.stairsClimbed,

            breakfast = report.breakfast,
            lunch = report.lunch,
            dinner = report.dinner
        )
    }
}