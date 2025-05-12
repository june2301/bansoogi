package com.ddc.bansoogi.calendar.data.local

import android.util.Log
import com.ddc.bansoogi.R
import com.ddc.bansoogi.calendar.data.entity.RecordedReport
import com.ddc.bansoogi.common.data.local.RealmManager
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class RecordedReportDataSource {
    private val realm = RealmManager.realm

    suspend fun createRecordedReport(recordedReport: RecordedReport): Unit {
        realm.write {
            copyToRealm(recordedReport)
        }
    }

    fun getRecordedReportList(): Flow<List<RecordedReport>> =
        realm.query<RecordedReport>()
            .asFlow()
            .map { it.list }

    suspend fun getRecordedReportByDate(date: String): RecordedReport? {
        return realm.query<RecordedReport>("reportedDate == $0", date)
            .asFlow()
            .map { it.list.firstOrNull() }
            .firstOrNull()
    }

    // bansoogi에 대한 data 호출 부분이 생성된다면 삭제 예정임
    fun getBansoogiById(id: Int): Bansoogi? {
        return realm.query<Bansoogi>("bansoogiId == $0", id).first().find()
    }

    // 더미데이터
    suspend fun initialize() {
        val hasBonsoogi = realm.query<Bansoogi>().find().isNotEmpty()

        if (!hasBonsoogi) {
            realm.write {
                copyToRealm(Bansoogi().apply {
                    bansoogiId = 1
                    title = "기본 반숙이"
                    imageUrl = R.drawable.bansoogi_basic
                    silhouetteImageUrl = R.drawable.bansoogi_basic
                    gifUrl = R.drawable.bansoogi_basic
                    description = "기본 반숙이 캐릭터입니다. 아직 성장 전의 모습이에요!"
                })

                copyToRealm(Bansoogi().apply {
                    bansoogiId = 2
                    title = "걷는 반숙이"
                    imageUrl = R.drawable.bansoogi_walk
                    silhouetteImageUrl = R.drawable.bansoogi_walk
                    gifUrl = R.drawable.bansoogi_walk
                    description = "걷는 반숙이 캐릭터입니다. 다리가 아주 짧군요!"
                })
            }
        }

        // id 확인을 위한 임시 출력
        val bansoogiList = realm.query<Bansoogi>().find()
        bansoogiList.forEach { bansoogi ->
            Log.d("Bansoogi Debug", "ID: ${bansoogi.bansoogiId}, 반숙이 이름: ${bansoogi.title}")
        }

        val hasRecordedReport = realm.query<RecordedReport>().find().isNotEmpty()

        if (!hasRecordedReport) {
            realm.write {
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 90
                    bansoogiId = 1
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
                    reportedDate = "2025-04-26"
                })

                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 70
                    bansoogiId = 1
                    standupCount = 2
                    stretchCount = 1
                    phoneOffCount = 0
                    lyingTime = 240
                    sittingTime = 700
                    phoneTime = 280
                    sleepTime = 400
                    walkCount = 3000
                    runTime = 0
                    exerciseTime = 20
                    stairsClimbed = 2
                    breakfast = true
                    lunch = false
                    dinner = true
                    reportedDate = "2025-04-27"
                })

                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 100
                    bansoogiId = 2
                    standupCount = 4
                    stretchCount = 3
                    phoneOffCount = 2
                    lyingTime = 120
                    sittingTime = 500
                    phoneTime = 120
                    sleepTime = 480
                    walkCount = 7000
                    runTime = 15
                    exerciseTime = 40
                    stairsClimbed = 8
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-04-28"
                })

                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 50
                    bansoogiId = 2
                    standupCount = 1
                    stretchCount = 0
                    phoneOffCount = 0
                    lyingTime = 300
                    sittingTime = 800
                    phoneTime = 400
                    sleepTime = 360
                    walkCount = 1500
                    runTime = 0
                    exerciseTime = 5
                    stairsClimbed = 1
                    breakfast = false
                    lunch = false
                    dinner = true
                    reportedDate = "2025-04-29"
                })
            }
        }
    }
}