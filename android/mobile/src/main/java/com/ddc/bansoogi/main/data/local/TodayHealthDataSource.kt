package com.ddc.bansoogi.main.data.local

import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.main.data.entity.TodayHealthData
import io.realm.kotlin.ext.query

class TodayHealthDataSource {
    private val realm = RealmManager.realm


    suspend fun createTodayHealthData(log: TodayHealthData) {
        realm.write {
            copyToRealm(log)
        }
    }

    fun getTodayHealthDataByDate(date: String): TodayHealthData? {
        return realm.query<TodayHealthData>("reportedDate == $0", date)
            .find().firstOrNull()
    }

    suspend fun initialize() {
        val hasTodayHealth = realm.query<TodayHealthData>().find().isNotEmpty()

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
}