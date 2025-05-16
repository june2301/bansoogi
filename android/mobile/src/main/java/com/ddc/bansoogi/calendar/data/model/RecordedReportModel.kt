package com.ddc.bansoogi.calendar.data.model

import android.util.Log
import com.ddc.bansoogi.calendar.data.entity.RecordedReport
import com.ddc.bansoogi.calendar.data.local.RecordedReportDataSource
import com.ddc.bansoogi.collection.data.model.CollectionModel
import com.ddc.bansoogi.common.data.model.ActivityLogModel
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import io.realm.kotlin.types.RealmInstant
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
        runTimeData: Int,
        exerciseTimeData: Int,
        stairsClimbedData: Int
    ) {
        val date = todayRecordDto.createdAt.toLocalDate()

        val actualBansoogiId = if (todayRecordDto.energyPoint >= 80) {
            bansoogiIdData
        } else {
            0
        }

        dataSource.createRecordedReport(
            RecordedReport().apply {
                finalEnergyPoint = todayRecordDto.energyPoint
                bansoogiId = actualBansoogiId

                standupCount = todayRecordDto.standUpCnt
                stretchCount = todayRecordDto.stretchCnt
                phoneOffCount = todayRecordDto.phoneOffCnt

                lyingTime = todayRecordDto.lyingTime
                sittingTime = todayRecordDto.sittingTime
                phoneTime = todayRecordDto.phoneTime
                sleepTime = todayRecordDto.sleepTime

                walkCount = walkCountData
                runTime = runTimeData
                exerciseTime = exerciseTimeData
                stairsClimbed = stairsClimbedData

                breakfast = todayRecordDto.breakfast
                lunch = todayRecordDto.lunch
                dinner = todayRecordDto.dinner

                reportedDate = "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
            }
        )
    }

    fun getCalendarMarkers(): List<CalendarMarkerDto> {
        return dataSource.getRecordedReportList().map { report ->
            val bansoogi = CollectionModel().getBansoogiById(report.bansoogiId)

            CalendarMarkerDto(
                date = LocalDate.parse(report.reportedDate),
                bansoogiGifUrl =  bansoogi.gifUrl,
                bansoogiImageUrl = bansoogi.imageUrl
            )
        }
    }

    fun getDetailReport(date: String): DetailReportDto? {
        val report = dataSource.getRecordedReportByDate(date) ?: return null

        val bansoogi = CollectionModel().getBansoogiById(report.bansoogiId)

        // 로그 호출
        val standLog = logModel.getLogsByTypeAndDate("STANDUP", date)
        val stretchLog = logModel.getLogsByTypeAndDate("STRETCH", date)
        val phoneOffLog = logModel.getLogsByTypeAndDate("PHONE_OFF", date)

        return DetailReportDto(
            date = report.reportedDate,

            finalEnergyPoint = report.finalEnergyPoint,

            bansoogiTitle = bansoogi.title,
            bansoogiGifUrl = bansoogi.gifUrl,
            bansoogiImageUrl = bansoogi.imageUrl,

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

    fun getLatestRecordedReport(): DetailReportDto {
        val report = dataSource.getLatestRecordedReport()

        val bansoogi = CollectionModel().getBansoogiById(report.bansoogiId)

        // 로그 호출
        val standLog = logModel.getLogsByTypeAndDate("STANDUP", report.reportedDate)
        val stretchLog = logModel.getLogsByTypeAndDate("STRETCH", report.reportedDate)
        val phoneOffLog = logModel.getLogsByTypeAndDate("PHONE_OFF", report.reportedDate)

        return DetailReportDto(
            date = report.reportedDate,

            finalEnergyPoint = report.finalEnergyPoint,

            bansoogiTitle = bansoogi.title,
            bansoogiGifUrl = bansoogi.gifUrl,
            bansoogiImageUrl = bansoogi.imageUrl,

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