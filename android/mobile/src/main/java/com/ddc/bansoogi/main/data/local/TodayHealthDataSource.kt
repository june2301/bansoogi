package com.ddc.bansoogi.main.data.local

import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.main.data.entity.TodayHealthData
import io.realm.kotlin.ext.query

class TodayHealthDataSource {
    private val realm = RealmManager.realm

    suspend fun initialize(date: String) {
        val hasTodayHealth = realm.query<TodayHealthData>("recordedDate == $0", date).find().isNotEmpty()

        if (hasTodayHealth) return

        val dummyTodayHealth = listOf(
            TodayHealthData().apply {
                stepGoal = 0
                steps = 0
                floorsClimbed = 0
                sleepTime = 0
                exerciseTime = 0
            }
        )

        realm.write {
            dummyTodayHealth.forEach { copyToRealm(it) }
        }
    }

    suspend fun createTodayHealthData(log: TodayHealthData) {
        realm.write {
            copyToRealm(log)
        }
    }

    fun getTodayHealthDataByDate(date: String): TodayHealthData? {
        return realm.query<TodayHealthData>("recordedDate == $0", date)
            .find().firstOrNull()
    }

    suspend fun updateTodayHealthDataByDate(date: String, stepGoal: Int?, steps: Int?, floorsClimbed: Int?, sleepTime: Int?, exerciseTime: Int?) {
        realm.write {
            val existTodayHealth = query<TodayHealthData>("recordedDate == $0", date)
                .first()
                .find()
            if (existTodayHealth!=null) {
                findLatest(existTodayHealth)?.apply {
                    this.stepGoal = stepGoal
                    this.steps = steps
                    this.floorsClimbed = floorsClimbed
                    this.sleepTime = sleepTime
                    this.exerciseTime = exerciseTime
                }
            } else {
                copyToRealm(TodayHealthData().apply {
                    this.recordedDate = date
                    this.stepGoal = stepGoal
                    this.steps = steps
                    this.floorsClimbed = floorsClimbed
                    this.sleepTime = sleepTime
                    this.exerciseTime = exerciseTime
                })
            }
        }
    }
}