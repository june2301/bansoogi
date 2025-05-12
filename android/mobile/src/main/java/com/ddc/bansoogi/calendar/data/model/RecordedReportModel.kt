package com.ddc.bansoogi.calendar.data.model

import com.ddc.bansoogi.calendar.data.entity.RecordedReport
import com.ddc.bansoogi.calendar.data.local.Bansoogi
import com.ddc.bansoogi.calendar.data.local.RecordedReportDataSource
import com.ddc.bansoogi.common.data.model.ActivityLogModel
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.Int

class RecordedReportModel {
    private val dataSource = RecordedReportDataSource()
    private val logModel = ActivityLogModel()

    // 더미데이터용 추후 삭제 예정
    suspend fun initialize() {
        dataSource.initialize()
        logModel.initialize()
    }

    suspend fun createRecordedReport(
        todayRecordDto: TodayRecordDto,
        bansoogiIdData: Int,
        walkCountData: Int,
        runTileData: Int,
        exerciseTimeData: Int,
        stairsClimbedData: Int
    ) {
        val date = todayRecordDto.createdAt.toLocalDate()

        dataSource.createRecordedReport(
            RecordedReport().apply {
                finalEnergyPoint = todayRecordDto.energyPoint
                bansoogiId = bansoogiIdData

                standupCount = todayRecordDto.standUpCnt
                stretchCount = todayRecordDto.stretchCnt
                phoneOffCount = todayRecordDto.phoneOffCnt

                lyingTime = todayRecordDto.lyingTime
                sittingTime = todayRecordDto.sittingTime
                phoneTime = todayRecordDto.phoneTime
                sleepTime = todayRecordDto.sleepTime

                walkCount = walkCountData
                runTime = runTileData
                exerciseTime = exerciseTimeData
                stairsClimbed = stairsClimbedData

                breakfast = todayRecordDto.breakfast
                lunch = todayRecordDto.lunch
                dinner = todayRecordDto.dinner

                reportedDate = "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
            }
        )
    }

    fun getCalendarMarkers(): Flow<List<CalendarMarkerDto>> =
        dataSource.getRecordedReportList().map { reportList ->
            reportList.map { report ->
                // 나중에 반숙이 클래스에 대한 data 함수들이 만들어진다면 그 함수들로 변경할 예정
                val bansoogi = dataSource.getBansoogiById(report.bansoogiId)

                CalendarMarkerDto(
                    date = LocalDate.parse(report.reportedDate),
                    bansoogiAnimationId =  bansoogi?.gifUrl
                )
            }
        }

    suspend fun getDetailReport(date: String): DetailReportDto? {
        val report = dataSource.getRecordedReportByDate(date) ?: return null

        // 반숙이 데이터 호출도 나중에 변경 예정
        val bansoogi = dataSource.getBansoogiById(report.bansoogiId)

        // 로그 호출
        val standLog = logModel.getLogsByTypeAndDate("STANDUP", date)
        val stretchLog = logModel.getLogsByTypeAndDate("STRETCH", date)
        val phoneOffLog = logModel.getLogsByTypeAndDate("PHONE_OFF", date)

        return DetailReportDto(
            date = report.reportedDate,

            finalEnergyPoint = report.finalEnergyPoint,

            bansoogiTitle = bansoogi?.title ?: "",
            bansoogiResource = bansoogi?.gifUrl ?: 0,

            standupCount = report.standupCount,
            standLog = standLog,

            stretchCount = report.stretchCount,
            stretchLog = stretchLog,

            phoneOffCount = report.phoneOffCount,
            phoneOffLog = phoneOffLog,

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

fun RealmInstant.toLocalDate(): LocalDate {
    val instant = Instant.ofEpochSecond(this.epochSeconds, this.nanosecondsOfSecond.toLong())
    return instant.atZone(ZoneId.systemDefault()).toLocalDate()
}