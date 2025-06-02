package com.ddc.bansoogi.calendar.data.local

import com.ddc.bansoogi.calendar.data.entity.RecordedReport
import com.ddc.bansoogi.common.data.local.RealmManager
import io.realm.kotlin.ext.query
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    // 날짜 순 정렬해서 가장 첫 번째 반환
    fun getLatestRecordedReport(): RecordedReport {
        val reports = getRecordedReportList().sortedByDescending {
            LocalDate.parse(it.reportedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }

        return reports.first()
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
                        walkCount = 5000
                        stairsClimbed = 300
                        sleepTime = 5
                        exerciseTime = 30
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
                        walkCount = 5000
                        stairsClimbed = 300
                        sleepTime = 5
                        exerciseTime = 30
                        breakfast = true
                        lunch = true
                        dinner = true
                        reportedDate = "2025-04-%02d".format(day + 20)
                    })
                }
            }

            dummyData()
        }
    }

    suspend fun dummyData() {
        if (true) {
            realm.write {
                // 5월 1일 - 70점 (80점 이하 1)
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 70
                    bansoogiId = 0         // 80점 미만이므로 반숙이 없음
                    standupCount = 2
                    stretchCount = 1
                    phoneOffCount = 1
                    lyingTime = 190
                    sittingTime = 610
                    phoneTime = 220
                    walkCount = 3500
                    stairsClimbed = 250
                    sleepTime = 380
                    exerciseTime = 15
                    breakfast = true
                    lunch = false
                    dinner = false
                    reportedDate = "2025-05-01"
                })

                // 5월 2일 - 85점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 85
                    bansoogiId = 11
                    standupCount = 3
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 160
                    sittingTime = 550
                    phoneTime = 190
                    walkCount = 6000
                    stairsClimbed = 320
                    sleepTime = 420
                    exerciseTime = 30
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-02"
                })

                // 5월 3일 - 87점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 87
                    bansoogiId = 13
                    standupCount = 4
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 155
                    sittingTime = 540
                    phoneTime = 180
                    walkCount = 6200
                    stairsClimbed = 330
                    sleepTime = 425
                    exerciseTime = 35
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-03"
                })

                // 5월 4일 - 90점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 90
                    bansoogiId = 31
                    standupCount = 4
                    stretchCount = 3
                    phoneOffCount = 2
                    lyingTime = 150
                    sittingTime = 530
                    phoneTime = 170
                    walkCount = 6500
                    stairsClimbed = 350
                    sleepTime = 430
                    exerciseTime = 40
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-04"
                })

                // 5월 5일 - 100점 (100점 1)
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 100
                    bansoogiId = 32
                    standupCount = 5
                    stretchCount = 4
                    phoneOffCount = 3
                    lyingTime = 120
                    sittingTime = 480
                    phoneTime = 150
                    walkCount = 8000
                    stairsClimbed = 400
                    sleepTime = 450
                    exerciseTime = 60
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-05"
                })

                // 5월 6일 - 88점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 88
                    bansoogiId = 33
                    standupCount = 4
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 155
                    sittingTime = 540
                    phoneTime = 180
                    walkCount = 6300
                    stairsClimbed = 340
                    sleepTime = 425
                    exerciseTime = 35
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-06"
                })

                // 5월 7일 - 92점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 92
                    bansoogiId = 5
                    standupCount = 4
                    stretchCount = 3
                    phoneOffCount = 3
                    lyingTime = 140
                    sittingTime = 520
                    phoneTime = 160
                    walkCount = 7000
                    stairsClimbed = 370
                    sleepTime = 440
                    exerciseTime = 45
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-07"
                })

                // 5월 8일 - 86점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 86
                    bansoogiId = 31
                    standupCount = 3
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 160
                    sittingTime = 550
                    phoneTime = 185
                    walkCount = 6100
                    stairsClimbed = 325
                    sleepTime = 420
                    exerciseTime = 32
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-08"
                })

                // 5월 9일 - 89점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 89
                    bansoogiId = 7
                    standupCount = 4
                    stretchCount = 3
                    phoneOffCount = 2
                    lyingTime = 145
                    sittingTime = 530
                    phoneTime = 175
                    walkCount = 6400
                    stairsClimbed = 345
                    sleepTime = 435
                    exerciseTime = 40
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-09"
                })

                // 5월 10일 - 100점 (100점 2)
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 100
                    bansoogiId = 8
                    standupCount = 5
                    stretchCount = 4
                    phoneOffCount = 3
                    lyingTime = 120
                    sittingTime = 480
                    phoneTime = 150
                    walkCount = 8000
                    stairsClimbed = 400
                    sleepTime = 450
                    exerciseTime = 60
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-10"
                })

                // 5월 11일 - 94점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 94
                    bansoogiId = 11
                    standupCount = 5
                    stretchCount = 3
                    phoneOffCount = 3
                    lyingTime = 135
                    sittingTime = 510
                    phoneTime = 160
                    walkCount = 7200
                    stairsClimbed = 380
                    sleepTime = 445
                    exerciseTime = 50
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-11"
                })

                // 5월 12일 - 91점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 91
                    bansoogiId = 12
                    standupCount = 4
                    stretchCount = 3
                    phoneOffCount = 3
                    lyingTime = 140
                    sittingTime = 520
                    phoneTime = 165
                    walkCount = 6800
                    stairsClimbed = 365
                    sleepTime = 440
                    exerciseTime = 45
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-12"
                })

                // 5월 13일 - 65점 (80점 이하 2)
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 65
                    bansoogiId = 0         // 80점 미만이므로 반숙이 없음
                    standupCount = 2
                    stretchCount = 1
                    phoneOffCount = 1
                    lyingTime = 195
                    sittingTime = 620
                    phoneTime = 230
                    walkCount = 3200
                    stairsClimbed = 230
                    sleepTime = 370
                    exerciseTime = 10
                    breakfast = false
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-13"
                })

                // 5월 14일 - 93점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 93
                    bansoogiId = 12
                    standupCount = 5
                    stretchCount = 3
                    phoneOffCount = 3
                    lyingTime = 135
                    sittingTime = 510
                    phoneTime = 160
                    walkCount = 7100
                    stairsClimbed = 375
                    sleepTime = 445
                    exerciseTime = 48
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-14"
                })

                // 5월 15일 - 100점 (100점 3)
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 100
                    bansoogiId = 15
                    standupCount = 5
                    stretchCount = 4
                    phoneOffCount = 3
                    lyingTime = 120
                    sittingTime = 480
                    phoneTime = 150
                    walkCount = 8000
                    stairsClimbed = 400
                    sleepTime = 450
                    exerciseTime = 60
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-15"
                })

                // 5월 16일 - 55점 (80점 이하 3)
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 55
                    bansoogiId = 0         // 80점 미만이므로 반숙이 없음
                    standupCount = 1
                    stretchCount = 1
                    phoneOffCount = 1
                    lyingTime = 210
                    sittingTime = 640
                    phoneTime = 250
                    walkCount = 2800
                    stairsClimbed = 200
                    sleepTime = 360
                    exerciseTime = 0
                    breakfast = false
                    lunch = false
                    dinner = true
                    reportedDate = "2025-05-16"
                })

                // 5월 17일 - 84점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 84
                    bansoogiId = 4
                    standupCount = 3
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 165
                    sittingTime = 560
                    phoneTime = 195
                    walkCount = 5800
                    stairsClimbed = 310
                    sleepTime = 415
                    exerciseTime = 28
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-17"
                })

                // 5월 18일 - 82점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 82
                    bansoogiId = 7
                    standupCount = 3
                    stretchCount = 2
                    phoneOffCount = 1
                    lyingTime = 170
                    sittingTime = 580
                    phoneTime = 200
                    walkCount = 5500
                    stairsClimbed = 300
                    sleepTime = 410
                    exerciseTime = 25
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-18"
                })

                // 5월 19일 - 100점 (100점 4)
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 100
                    bansoogiId = 13
                    standupCount = 5
                    stretchCount = 4
                    phoneOffCount = 3
                    lyingTime = 120
                    sittingTime = 480
                    phoneTime = 150
                    walkCount = 8000
                    stairsClimbed = 400
                    sleepTime = 450
                    exerciseTime = 60
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-19"
                })

                // 5월 20일 - 96점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 96
                    bansoogiId = 15
                    standupCount = 5
                    stretchCount = 3
                    phoneOffCount = 3
                    lyingTime = 130
                    sittingTime = 500
                    phoneTime = 155
                    walkCount = 7500
                    stairsClimbed = 390
                    sleepTime = 448
                    exerciseTime = 55
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-20"
                })

                // 5월 21일 - 88점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 88
                    bansoogiId = 32
                    standupCount = 4
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 155
                    sittingTime = 545
                    phoneTime = 180
                    walkCount = 6300
                    stairsClimbed = 335
                    sleepTime = 428
                    exerciseTime = 35
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-21"
                })

                // 5월 22일 - 88점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 88
                    bansoogiId = 10
                    standupCount = 4
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 155
                    sittingTime = 545
                    phoneTime = 180
                    walkCount = 6300
                    stairsClimbed = 335
                    sleepTime = 428
                    exerciseTime = 35
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-22"
                })

                // 5월 23일
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 78 // 80점 미만
                    bansoogiId = 0
                    standupCount = 2
                    stretchCount = 2
                    phoneOffCount = 1
                    lyingTime = 200
                    sittingTime = 630
                    phoneTime = 240
                    walkCount = 3200
                    stairsClimbed = 210
                    sleepTime = 360
                    exerciseTime = 10
                    breakfast = false
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-23"
                })

                // 5월 24일
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 89
                    bansoogiId = 6
                    standupCount = 4
                    stretchCount = 3
                    phoneOffCount = 2
                    lyingTime = 150
                    sittingTime = 520
                    phoneTime = 170
                    walkCount = 6400
                    stairsClimbed = 350
                    sleepTime = 430
                    exerciseTime = 40
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-24"
                })

                // 5월 25일
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 100
                    bansoogiId = 7
                    standupCount = 5
                    stretchCount = 4
                    phoneOffCount = 3
                    lyingTime = 120
                    sittingTime = 480
                    phoneTime = 150
                    walkCount = 8000
                    stairsClimbed = 400
                    sleepTime = 450
                    exerciseTime = 60
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-25"
                })

                // 5월 26일
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 85
                    bansoogiId = 33
                    standupCount = 4
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 160
                    sittingTime = 550
                    phoneTime = 185
                    walkCount = 6100
                    stairsClimbed = 325
                    sleepTime = 420
                    exerciseTime = 32
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-26"
                })

                // 5월 27일 - 90점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 90
                    bansoogiId = 12
                    standupCount = 4
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 150
                    sittingTime = 520
                    phoneTime = 170
                    walkCount = 6500
                    stairsClimbed = 350
                    sleepTime = 430
                    exerciseTime = 40
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-27"
                })

                // 5월 28일 - 95점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 95
                    bansoogiId = 16
                    standupCount = 5
                    stretchCount = 3
                    phoneOffCount = 3
                    lyingTime = 130
                    sittingTime = 500
                    phoneTime = 160
                    walkCount = 7200
                    stairsClimbed = 380
                    sleepTime = 445
                    exerciseTime = 50
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-28"
                })

                // 5월 29일 - 84점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 84
                    bansoogiId = 9
                    standupCount = 3
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 165
                    sittingTime = 560
                    phoneTime = 190
                    walkCount = 5800
                    stairsClimbed = 310
                    sleepTime = 415
                    exerciseTime = 28
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-29"
                })

                // 5월 30일 - 92점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 92
                    bansoogiId = 10
                    standupCount = 4
                    stretchCount = 3
                    phoneOffCount = 3
                    lyingTime = 140
                    sittingTime = 520
                    phoneTime = 165
                    walkCount = 7000
                    stairsClimbed = 370
                    sleepTime = 440
                    exerciseTime = 45
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-05-30"
                })

                // 5월 31일 - 87점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 87
                    bansoogiId = 6
                    standupCount = 3
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 160
                    sittingTime = 550
                    phoneTime = 180
                    walkCount = 6100
                    stairsClimbed = 325
                    sleepTime = 420
                    exerciseTime = 32
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-05-31"
                })

                // 6월 1일 - 100점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 100
                    bansoogiId = 34
                    standupCount = 5
                    stretchCount = 4
                    phoneOffCount = 3
                    lyingTime = 120
                    sittingTime = 480
                    phoneTime = 150
                    walkCount = 8000
                    stairsClimbed = 400
                    sleepTime = 450
                    exerciseTime = 60
                    breakfast = true
                    lunch = true
                    dinner = true
                    reportedDate = "2025-06-01"
                })

                // 6월 2일 - 90점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 90
                    bansoogiId = 7
                    standupCount = 4
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 150
                    sittingTime = 520
                    phoneTime = 170
                    walkCount = 6500
                    stairsClimbed = 350
                    sleepTime = 430
                    exerciseTime = 40
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-06-02"
                })

                // 6월 3일 - 85점
                copyToRealm(RecordedReport().apply {
                    finalEnergyPoint = 85
                    bansoogiId = 33
                    standupCount = 4
                    stretchCount = 2
                    phoneOffCount = 2
                    lyingTime = 160
                    sittingTime = 550
                    phoneTime = 185
                    walkCount = 6100
                    stairsClimbed = 325
                    sleepTime = 420
                    exerciseTime = 32
                    breakfast = true
                    lunch = true
                    dinner = false
                    reportedDate = "2025-06-03"
                })
            }
        }
    }
}