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
}