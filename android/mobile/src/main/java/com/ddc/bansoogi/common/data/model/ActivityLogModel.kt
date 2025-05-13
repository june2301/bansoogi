package com.ddc.bansoogi.common.data.model

import com.ddc.bansoogi.common.data.entity.ActivityLog
import com.ddc.bansoogi.common.data.local.ActivityLogDataSource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ActivityLogModel {
    private val dataSource = ActivityLogDataSource()

    // 더미데이터용 추후 삭제 예정
    suspend fun initialize() {
        dataSource.initialize()
    }

    // TYPE : "STANDUP", "STRETCH", "PHONE_OFF"
    // fromBehavior: "SITTING", "LYING", "PHONE_IN_USE"
    suspend fun createActivicyLog(
        activityType: String,
        fromBehavior: String,
        durationTime: Int
    ) {
        val dateTime = LocalDateTime.now()
        val date = dateTime.toLocalDate()
        val time = dateTime.toLocalTime()

        dataSource.createActivityLog(
            ActivityLog().apply {
                type = activityType
                fromState = fromBehavior
                duration = durationTime
                reactedDate = "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
                reactedTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))
            },
        )
    }

    fun getLogsByTypeAndDate(type: String, date: String): List<ActivityLogDto> {
        return dataSource.getLogsByTypeAndDate(type, date)
            .map {
                it.toDto()
            }
    }

    fun ActivityLog.toDto(): ActivityLogDto {
        return ActivityLogDto(
            type = this.type,
            fromState = this.fromState,
            duration = this.duration,
            reactedTime = this.reactedTime
        )
    }
}