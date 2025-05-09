package com.ddc.bansoogi.common.util.health

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * [RealTimeHealthDataManager]
 * 실시간으로 HealthData를 가져오기 위한 데이터 Manager
 */
class RealTimeHealthDataManager(private val healthDataStore: HealthDataStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 데이터를 저장할 StateFlow
    private val _healthData = MutableStateFlow(CustomHealthData(0L, 0, 0.0f, 0))
    val healthData: Flow<CustomHealthData> = _healthData.asStateFlow()

    // 업데이트 간격 (밀리초)
    private var updateInterval = 10000L // 10초

    // 데이터 수집 작업 실행 중 여부
    private var isCollecting = false

    // 데이터 수집 시작
    fun startCollecting() {
        if (isCollecting) return

        isCollecting = true
        scope.launch {

            while (isActive && isCollecting) {
                try {
                    // 데이터 가져오기
                    val stepGoal = readLastStepGoal(healthDataStore)
                    val todaySteps = readStepData(healthDataStore)
                    val floorsClimbed = readFloorsClimbed(healthDataStore)
                    val sleepData = readSleepData(healthDataStore)

                    // 데이터 업데이트
                    _healthData.value = CustomHealthData(todaySteps, stepGoal, floorsClimbed, sleepData)
                } catch (e: Exception) {
                    Log.e("HEALTH_DATA", "Error collecting data: ${e.message}", e)
                }

                // 다음 업데이트까지 대기
                delay(updateInterval)
            }
        }
    }

    // 데이터 수집 중지
    fun stopCollecting() {
        isCollecting = false
    }

    // 수동으로 데이터 즉시 갱신
    fun refreshData() {
        scope.launch {
            try {
                val stepGoal = readLastStepGoal(healthDataStore)
                val todaySteps = readStepData(healthDataStore)
                val floorsClimbed = readFloorsClimbed(healthDataStore)
                val sleepData = readSleepData(healthDataStore)
                _healthData.value = CustomHealthData(todaySteps, stepGoal, floorsClimbed, sleepData)
            } catch (e: Exception) {
                Log.e("HEALTH_DATA", "Error refreshing data: ${e.message}", e)
            }
        }
    }

    // 업데이트 간격 설정
    fun setUpdateInterval(intervalMs: Long) {
        updateInterval = intervalMs
    }
}