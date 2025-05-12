package com.ddc.bansoogi.common.data.local

import com.ddc.bansoogi.calendar.data.entity.RecordedReport
import com.ddc.bansoogi.calendar.data.local.Bansoogi
import com.ddc.bansoogi.common.data.entity.ActivityLog
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ActivityLogDataSource {
    private val realm = RealmManager.realm

    suspend fun createActivityLog(log: ActivityLog) {
        realm.write {
            copyToRealm(log)
        }
    }

    fun getLogsByTypeAndDate(type: String, date: String): List<ActivityLog> {
        return realm.query<ActivityLog>(
            "type == $0 AND reactedDate == $1", type, date)
            .find()
    }

    // 더미데이터
    suspend fun initialize() {
        val hasLogs = realm.query<ActivityLog>().find().isNotEmpty()

        if (hasLogs) return

        val dummyLogs = listOf(
            ActivityLog().apply {
                type = "STANDUP"
                fromState = "SITTING"
                duration = 10
                reactedDate = "2025-04-29"
                reactedTime = "07:30"
            },
            ActivityLog().apply {
                type = "STANDUP"
                fromState = "LYING"
                duration = 60
                reactedDate = "2025-04-29"
                reactedTime = "13:20"
            },
            ActivityLog().apply {
                type = "STRETCH"
                fromState = "LYING"
                duration = 20
                reactedDate = "2025-04-29"
                reactedTime = "12:10"
            },
            ActivityLog().apply {
                type = "PHONE_OFF"
                fromState = "PHONE_IN_USE"
                duration = 1
                reactedDate = "2025-04-29"
                reactedTime = "18:50"
            }
        )

        realm.write {
            dummyLogs.forEach { copyToRealm(it) }
        }
    }
}