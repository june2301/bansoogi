package com.ddc.bansoogi.main.util.health

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.LocalDate
import java.time.LocalDateTime

object Permissions {
    val PERMISSIONS: Set<Permission> = setOf(
        Permission.of(DataTypes.STEPS, AccessType.READ),
        Permission.of(DataTypes.STEPS_GOAL, AccessType.READ)
    )
}

@Throws(HealthDataException::class)
suspend fun readStepData(healthDataStore: HealthDataStore): Long {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(1)
    Log.d("STEPS", "Reading step data from $startDate to $endDate")

    val startTime = LocalDateTime.of(startDate, java.time.LocalTime.MIN)
    val endTime = LocalDateTime.of(endDate, java.time.LocalTime.MAX)

    val readRequest = DataType.StepsType.TOTAL.requestBuilder
        .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
        .build()

    val result = healthDataStore.aggregateData(readRequest)
    var totalSteps = 0L

    result.dataList.forEach { data ->
        Log.d("STEPS", "Steps: ${data.value} from ${data.startTime} to ${data.endTime}")
        data.value?.let { totalSteps = it }
    }

    return totalSteps
}

@Throws(HealthDataException::class)
suspend fun readLastStepGoal(healthDataStore: HealthDataStore): Int {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(1)
    Log.d("HELLO", "StartDate: $startDate; EndDate: $endDate")
    val readRequest = DataType.StepsGoalType.LAST
        .requestBuilder
        .setLocalDateFilter(LocalDateFilter.of(startDate, endDate))
        .build()
    val result = healthDataStore.aggregateData(readRequest)
    var stepGoal = 0
    result.dataList.forEach { data ->
        Log.d("HELLO", "Step Goal: ${data.value}")

        Log.d("HELLO", "data.startTime: ${data.startTime}")
        Log.d("HELLO", "data.endTime: ${data.endTime}")
        data.value?.let { stepGoal = it }
    }
    return stepGoal
}