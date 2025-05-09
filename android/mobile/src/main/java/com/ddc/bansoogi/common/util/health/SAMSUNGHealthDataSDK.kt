package com.ddc.bansoogi.common.util.health

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
import java.time.LocalTime

object Permissions {
    val PERMISSIONS: Set<Permission> = setOf(
        Permission.of(DataTypes.STEPS, AccessType.READ), // 걸음수
        Permission.of(DataTypes.STEPS_GOAL, AccessType.READ),
        Permission.of(DataTypes.SLEEP, AccessType.READ), // 수면
        Permission.of(DataTypes.SLEEP_GOAL, AccessType.READ), // 수면 목표 시간
        Permission.of(DataTypes.FLOORS_CLIMBED, AccessType.READ), // 계단
        Permission.of(DataTypes.HEART_RATE, AccessType.READ) // 심박수
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
suspend fun readSleepData(healthDataStore: HealthDataStore): Int {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(1)

    val readLAST_BED_TIMERequest = DataType.SleepGoalType.LAST_BED_TIME
        .requestBuilder
        .setLocalDateFilter(LocalDateFilter.of(startDate, endDate))
        .build()

    val readLAST_WAKE_UP_TIMERequest = DataType.SleepGoalType.LAST_WAKE_UP_TIME
        .requestBuilder
        .setLocalDateFilter(LocalDateFilter.of(startDate, endDate))
        .build()

    val lastBedTime = healthDataStore.aggregateData(readLAST_BED_TIMERequest)
    val lastWakeUpTime = healthDataStore.aggregateData(readLAST_WAKE_UP_TIMERequest)

    var sleepBedTimeMinutes = LocalTime.now()
    var sleepWakeUpMinutes = LocalTime.now()

    lastBedTime.dataList.forEach { data ->
        data.value?.let { sleepBedTimeMinutes = it }
    }
    lastWakeUpTime.dataList.forEach { data ->
        data.value?.let { sleepWakeUpMinutes = it }
    }

    val bedTimeMinutes = sleepBedTimeMinutes.hour * 60 + sleepBedTimeMinutes.minute
    val wakeUpMinutes = sleepWakeUpMinutes.hour * 60 + sleepWakeUpMinutes.minute

// 기상 시간이 취침 시간보다 이른 경우(다음 날 기상) 24시간(1440분) 추가
    val totalMinutes = if (wakeUpMinutes < bedTimeMinutes) {
        (wakeUpMinutes + 1440) - bedTimeMinutes
    } else {
        wakeUpMinutes - bedTimeMinutes
    }
    return totalMinutes
}
