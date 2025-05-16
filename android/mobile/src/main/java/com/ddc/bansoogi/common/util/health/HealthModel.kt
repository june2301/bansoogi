package com.ddc.bansoogi.common.util.health

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.main.controller.TodayHealthDataController
import com.ddc.bansoogi.main.controller.TodayRecordController
import com.samsung.android.sdk.health.data.HealthDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId

/**
 * [RealTimeHealthDataManager]
 * 실시간으로 HealthData를 가져오기 위한 데이터 Manager
 */
class RealTimeHealthDataManager(private val healthDataStore: HealthDataStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 데이터를 저장할 StateFlow
    private val _healthData = MutableStateFlow(CustomHealthData(0L, 0, 0.0f, 0, 0))
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
                    val steps = readStepData(healthDataStore)
                    val floorsClimbed = readFloorsClimbed(healthDataStore)
                    val sleepTime = readSleepData(healthDataStore)
                    val exerciseTime = readExerciseData(healthDataStore)

                    // 데이터 업데이트
                    _healthData.value = CustomHealthData(steps, stepGoal, floorsClimbed, sleepTime, exerciseTime)

                    // TodayHealthData 갱신
                    // TODO: TodayRecord energyPoint 갱신, 날짜 오늘 날짜로 변경
                    val previousHealth = TodayHealthDataController().getTodayHealthData("2025-05-16")
                    Log.d("TODAY_HEALTH_DATA", "id: ${previousHealth?.id}, stepGoal: ${previousHealth?.stepGoal}" +
                            ", steps: ${previousHealth?.steps}, floorsClimbed: ${previousHealth?.floorsClimbed}" +
                            ", sleepTime: ${previousHealth?.sleepTime}, exerciseTime: ${previousHealth?.exerciseTime}")
                    // TODO: 날짜 오늘 날짜로 변경
                    TodayHealthDataController().updateTodayHealthData("2025-05-16", stepGoal, steps.toInt(), floorsClimbed.toInt(), sleepTime, exerciseTime)

                    val todayRecordModel = TodayRecordModel()
                    val todayRecord = todayRecordModel.getTodayRecordSync()
                    var recordId:ObjectId = ObjectId()
                    todayRecord?.let {
                        // 여기서 todayRecord 데이터 사용
                        recordId = it.recordId
                    }
                    // step
                    if (_healthData.value.step > (previousHealth?.steps ?: 0)) {
                        val addedEnergy = calculateStep(_healthData.value.step.toInt()) - calculateStep(previousHealth?.steps)
                        todayRecordModel.updateEnergy(recordId, addedEnergy)
                    }
                    // floorsClimbed
                    if (_healthData.value.floorsClimbed > (previousHealth?.floorsClimbed ?: 0)) {
                        val addedEnergy = calculateFloorsClimbed(_healthData.value.step.toInt()) - calculateFloorsClimbed(previousHealth?.steps)
                        todayRecordModel.updateEnergy(recordId, addedEnergy)
                    }
                    // exerciseTime
                    if ((_healthData.value.exerciseTime?:0) > (previousHealth?.exerciseTime ?: 0)) {
                        val addedEnergy = calculateExercise(_healthData.value.step.toInt()) - calculateExercise(previousHealth?.steps)
                        todayRecordModel.updateEnergy(recordId, addedEnergy)
                    }
                } catch (e: Exception) {
                    Log.e("HEALTH_DATA", "Error collecting data: ${e.message}", e)
                }

                // 다음 업데이트까지 대기
                delay(updateInterval)
            }
        }
    }

    fun calculateStep(step: Int?) : Int {
        step?.let {
            return (step / 1_000) * 5
        } ?: return 0
    }

    fun calculateFloorsClimbed(floors: Int?) : Int {
        floors?.let {
            return (floors / 5) * 10
        } ?: return 0
    }

    fun calculateExercise(exercise: Int?) : Int {
        exercise?.let {
            return (exercise / 15) * 30
        } ?: return 0
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
                val exerciseTime = readExerciseData(healthDataStore)
                _healthData.value = CustomHealthData(todaySteps, stepGoal, floorsClimbed, sleepData, exerciseTime)
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
