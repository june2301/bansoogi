package com.ddc.bansoogi.main.data.model

import com.ddc.bansoogi.main.data.local.TodayHealthDataSource


class TodayHealthDataModel {
    private val dataSource = TodayHealthDataSource()

    // 더미데이터용 추후 삭제 예정
    suspend fun initialize() {
        dataSource.initialize()
    }

    fun getTodayHealthData(date: String): TodayHealthDataDto? {
        val data = dataSource.getTodayHealthDataByDate(date) ?: return null

        return TodayHealthDataDto(
            id = data.id,
            stepGoal = data.stepGoal,
            steps = data.steps,
            floorsClimbed = data.floorsClimbed,
            sleepTime = data.sleepTime,
            exerciseTime = data.exerciseTime,
            recordedDate = data.recordedDate,
        )
    }
}