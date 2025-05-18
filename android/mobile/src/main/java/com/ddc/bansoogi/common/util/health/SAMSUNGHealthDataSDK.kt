package com.ddc.bansoogi.common.util.health

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.AggregatedData
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.response.DataResponse
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object Permissions {
    val PERMISSIONS: Set<Permission> = setOf(
        Permission.of(DataTypes.STEPS, AccessType.READ), // 걸음수
        Permission.of(DataTypes.STEPS_GOAL, AccessType.READ),
        Permission.of(DataTypes.SLEEP, AccessType.READ), // 수면
        Permission.of(DataTypes.SLEEP_GOAL, AccessType.READ), // 수면 목표 시간
        Permission.of(DataTypes.FLOORS_CLIMBED, AccessType.READ), // 계단
        Permission.of(DataTypes.HEART_RATE, AccessType.READ), // 심박수
        Permission.of(DataTypes.EXERCISE, AccessType.READ) // 운동
    )
}

@Throws(HealthDataException::class)
suspend fun readStepData(healthDataStore: HealthDataStore): Long {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(1)

    val startTime = LocalDateTime.of(startDate, LocalTime.MIN)
    val endTime = LocalDateTime.of(endDate, LocalTime.MAX)

    val readRequest = DataType.StepsType.TOTAL.requestBuilder
        .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
        .build()

    val result = healthDataStore.aggregateData(readRequest)
    var totalSteps = 0L

    result.dataList.forEach { data ->
        data.value?.let { totalSteps = it }
    }

    return totalSteps
}

@Throws(HealthDataException::class)
suspend fun readLastStepGoal(healthDataStore: HealthDataStore): Int {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(1)
    val readRequest = DataType.StepsGoalType.LAST
        .requestBuilder
        .setLocalDateFilter(LocalDateFilter.of(startDate, endDate))
        .build()
    val result = healthDataStore.aggregateData(readRequest)
    var stepGoal = 0
    result.dataList.forEach { data ->
        data.value?.let { stepGoal = it }
    }
    return stepGoal
}

@Throws(HealthDataException::class)
suspend fun readFloorsClimbed(healthDataStore: HealthDataStore): Float {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(1)

    val startTime = LocalDateTime.of(startDate, LocalTime.MIN)
    val endTime = LocalDateTime.of(endDate, LocalTime.MAX)

    val readRequest = DataType.FloorsClimbedType.TOTAL.requestBuilder
        .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
        .build()

    val result = healthDataStore.aggregateData(readRequest)
    var totalFloors = 0.0f
    result.dataList.forEach { data ->
        data.value?.let { totalFloors = it }
    }

    return totalFloors
}

@Throws(HealthDataException::class)
suspend fun readSleepData(healthDataStore: HealthDataStore): Int? {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(1)

    val readSleepRequest = DataType.SleepType.TOTAL_DURATION
        .requestBuilder
        .setLocalDateFilter(LocalDateFilter.of(startDate, endDate))
        .build()

    try {
        val sleepDataResponse: DataResponse<AggregatedData<Duration>> =
            healthDataStore.aggregateData(readSleepRequest)

        val sleepDuration = sleepDataResponse.dataList.firstOrNull()?.value

        if (sleepDuration == null) {
            Log.d("SLEEP_DATA", "수면 시간 값이 없습니다.")
            return null
        }

        val totalMinutes = sleepDuration.toMinutes().toInt()

        return totalMinutes
    } catch (e: Exception) {
        Log.e("SLEEP_DATA", "수면 데이터 조회 오류: ${e.message}", e)
        return null
    }
}

@Throws(HealthDataException::class)
suspend fun readExerciseData(healthDataStore: HealthDataStore): Int? {
    val today = LocalDate.now()

    val readSleepRequest = DataType.ExerciseType.TOTAL_DURATION
        .requestBuilder
        .setLocalDateFilter(LocalDateFilter.of(today, today.plusDays(1)))
        .build()

    try {
        val exerciseDataResponse: DataResponse<AggregatedData<Duration>> =
            healthDataStore.aggregateData(readSleepRequest)

        val exerciseDuration = exerciseDataResponse.dataList.firstOrNull()?.value

        if (exerciseDuration == null) {
            Log.d("EXERCISE_DATA", "운동 시간 값이 없습니다.")
            return null
        }

        val totalMinutes = exerciseDuration.toMinutes().toInt()

        return totalMinutes
    } catch (e: Exception) {
        Log.e("EXERCISE_DATA", "운동 데이터 조회 오류: ${e.message}", e)
        return null
    }
}
