package com.ddc.bansoogi.calendar.data.local

import android.util.Log
import com.ddc.bansoogi.calendar.data.entity.RecordedReport
import com.ddc.bansoogi.common.data.local.RealmManager
import io.realm.kotlin.ext.query

class RecordedReportDataSource {
    private val realm = RealmManager.realm

    suspend fun createRecordedReport(recordedReport: RecordedReport) {
        realm.write {
            copyToRealm(recordedReport)
        }
    }

    fun getRecordedReportList(): List<RecordedReport> {
        return realm.query<RecordedReport>()
            .find()
    }

    fun getRecordedReportByDate(date: String): RecordedReport? {
        return realm.query<RecordedReport>("reportedDate == $0", date)
            .find().firstOrNull()
    }

    // 더미데이터
    suspend fun initialize() {
        val hasRecordedReport = realm.query<RecordedReport>().find().isNotEmpty()

        if (!hasRecordedReport) {
            realm.write {
                for (day in 2..16) {
                    copyToRealm(RecordedReport().apply {
                        finalEnergyPoint = 90
                        bansoogiId = day
                        standupCount = 3
                        stretchCount = 2
                        phoneOffCount = 1
                        lyingTime = 180
                        sittingTime = 600
                        phoneTime = 200
                        sleepTime = 420
                        walkCount = 5000
                        runTime = 10
                        exerciseTime = 30
                        stairsClimbed = 5
                        breakfast = true
                        lunch = true
                        dinner = true
                        reportedDate = "2025-04-%02d".format(day)
                    })
                }

                for (day in 1..4) {
                    copyToRealm(RecordedReport().apply {
                        finalEnergyPoint = 90
                        bansoogiId = day + 30
                        standupCount = 3
                        stretchCount = 2
                        phoneOffCount = 1
                        lyingTime = 180
                        sittingTime = 600
                        phoneTime = 200
                        sleepTime = 420
                        walkCount = 5000
                        runTime = 10
                        exerciseTime = 30
                        stairsClimbed = 5
                        breakfast = true
                        lunch = true
                        dinner = true
                        reportedDate = "2025-04-%02d".format(day + 20)
                    })
                }
            }
        }
    }
}