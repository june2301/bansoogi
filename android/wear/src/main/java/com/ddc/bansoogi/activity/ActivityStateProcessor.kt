package com.ddc.bansoogi.activity

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.ddc.bansoogi.sensor.AndroidSensorManager

/* ─────────────── 최종 ActivityState 모델 ─────────────── */
sealed class ActivityState {
    object OffBody                  : ActivityState()
    object Sleeping                 : ActivityState()
    object Idle                     : ActivityState()               // StaticClassifier 적용 전
    data class Static(
        val type: StaticType,
        val phoneUsage: Boolean
    )                              : ActivityState()
    data class Dynamic(
        val type: DynamicType
    )                              : ActivityState()
    object Unknown                  : ActivityState()
}

//enum class StaticType { LYING, SITTING, STANDING }                 // (추후 구현)

/* ─────────────── PHONE_USAGE DTO ─────────────── */
data class PhoneUsageDto(val isUsing: Boolean)

/* ─────────────── ActivityStateProcessor 본체 ─────────────── */
class ActivityStateProcessor(
    private val sensorManager: AndroidSensorManager,
    externalScope: CoroutineScope? = null
) {
    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /* ① Off-body · Idle/Active 감지기 생성 */
    private var isOnBody = true
    private val idleActiveDetector = SmaIdleActiveDetector(
        linAcc = sensorManager.linearAcceleration,
        stepTimestamps = sensorManager.stepDetector,
        externalScope = scope
    )

    /* ② Classifiers */
    private val dynamicCls = DynamicClassifier(
        stepTimestamps = sensorManager.stepDetector,
        pressure       = sensorManager.pressure,
        linearAcceleration = sensorManager.linearAcceleration,
        heartRate      = sensorManager.heartRate,
        externalScope  = scope
    )
    // private val staticCls = StaticClassifier(...)

    /* ③ state flows */
    private val _state = MutableStateFlow<ActivityState>(ActivityState.Unknown)
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    private val sleepDetector = SleepDetector(
        linAcc    = sensorManager.linearAcceleration,
        heartRate = sensorManager.heartRate,
        externalScope = scope
    )

    /* ④ 시작/정지 */
    fun start() {
        sensorManager.startAll()

        collectOffBody()
        collectIdleActive()     // Idle/Active 토글
        collectDynamic()
        collectSleep()
    }

    fun stop() {
        sensorManager.stopAll()
        dynamicCls.stop()
        idleActiveDetector.stop()
        sleepDetector.stop()
        scope.cancel()
    }

    /* ───────── Collectors ───────── */

    private fun collectOffBody() = sensorManager.isOffBody
        .onEach { isOnBody = it
            sleepDetector.setEnabled(!isActive && isOnBody)
            recompute()
        }.launchIn(scope)         // true=착용


    private var isActive = false       // ACTIVE⇔IDLE
    private fun collectSleep() = sleepDetector.state
         .onEach { recompute() }
         .launchIn(scope)

    private fun collectIdleActive() = idleActiveDetector.state
        .onEach { state ->
            isActive = (state == SmaIdleActiveDetector.State.ACTIVE)
            dynamicCls.setEnabled(isActive)              // ACTIVE 때만 DynamicOn
            // staticCls.setEnabled(!isActive)
            // IDLE·착용 중일 때만 수면 감지 활성
            sleepDetector.setEnabled(!isActive && isOnBody)
            recompute()
        }.launchIn(scope)

    private var latestDynamic: DynamicType? = null
    private fun collectDynamic() = dynamicCls.state
        .onEach { latestDynamic = it; recompute() }
        .launchIn(scope)

    /* ───────── 최종 상태 합성 ───────── */
    private fun recompute() {
        val newState: ActivityState = when {
            !isOnBody                     -> ActivityState.OffBody
            sleepDetector.state.value is ActivityState.Sleeping
                                        -> ActivityState.Sleeping
            idleActiveDetector.state.value == SmaIdleActiveDetector.State.IDLE -> {
                // StaticClassifier 완성 전까지 Idle
                ActivityState.Idle
            }
            latestDynamic != null         -> ActivityState.Dynamic(latestDynamic!!)
            else                          -> ActivityState.Unknown
        }
        if (_state.value != newState) {
            _state.value = newState
            Log.d(TAG, "ActivityState → $newState")
        }
    }

    private companion object { const val TAG = "ActivityProcessor" }
}