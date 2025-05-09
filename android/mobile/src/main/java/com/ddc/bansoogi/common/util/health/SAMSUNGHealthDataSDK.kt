package com.ddc.bansoogi.common.util.health

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object Permissions {
    val PERMISSIONS: Set<Permission> = setOf(
        Permission.of(DataTypes.STEPS, AccessType.READ), // 걸음수
        Permission.of(DataTypes.STEPS_GOAL, AccessType.READ),
//        Permission.of(DataTypes.SLEEP, AccessType.READ), // 수면
//        Permission.of(DataTypes.SLEEP_GOAL, AccessType.READ), // 수면 목표 시간
//        Permission.of(DataTypes.FLOORS_CLIMBED, AccessType.READ), // 계단
    )
}

@Throws(HealthDataException::class)
suspend fun readStepData(healthDataStore: HealthDataStore): Long {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(1)
    Log.d("STEPS", "Reading step data from $startDate to $endDate")

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
    Log.d("STEPS_GOAL", "StartDate: $startDate; EndDate: $endDate")
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
