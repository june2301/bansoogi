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
//        val phoneUsage: Boolean
    )                              : ActivityState()
    data class Dynamic(
        val type: DynamicType
    )                              : ActivityState()
    object Unknown                  : ActivityState()
}

/* ─────────────── PHONE_USAGE DTO ─────────────── */
data class PhoneUsageDto(val isUsing: Boolean)

/* ─────────────── ActivityStateProcessor 본체 ─────────────── */
class ActivityStateProcessor(
    private val sensorManager: AndroidSensorManager,
    /** 폰 사용 여부를 워치로 브로드캐스트 받는 Flow (선택) */
    private val phoneUsage: Flow<PhoneUsageDto>? = null,           // ⭐
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
    private val staticCls = StaticClassifier(                        // ⭐ 추가
        linearAcceleration = sensorManager.linearAcceleration,
        heartRate          = sensorManager.heartRate,
        ppgGreen           = sensorManager.ppgGreen,                // (AndroidSensorManager에 ppgGreen Flow가 있다고 가정)
        externalScope      = scope
    )

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
        collectStatic()                // ⭐
        collectSleep()
//        collectPhoneUsage()            // ⭐
    }

    fun stop() {
        sensorManager.stopAll()
        staticCls.stop()              // ⭐
        dynamicCls.stop()
        idleActiveDetector.stop()
        sleepDetector.stop()
        scope.cancel()
    }

    /* ─────────── Collector 영역 ─────────── */

    /* (1) 착용 여부 */
    private fun collectOffBody() = sensorManager.isOffBody
        .onEach { isOnBody = it
            sleepDetector.setEnabled(!isActive && isOnBody)
            recompute()
        }.launchIn(scope)         // true=착용

    /* (2) 수면 */
    private fun collectSleep() = sleepDetector.state
         .onEach { recompute() }
         .launchIn(scope)

    /* (3) Idle / Active 전이 */
    private var isActive = false       // ACTIVE⇔IDLE
    private fun collectIdleActive() = idleActiveDetector.state
        .onEach { state ->
            isActive = (state == SmaIdleActiveDetector.State.ACTIVE)

            /* ACTIVE → Dynamic만 켜고 Static 꺼두기 */
            dynamicCls.setEnabled(isActive)              // ACTIVE 때만 DynamicOn
            staticCls.setEnabled(!isActive)      // ⭐ 반대로

            /* 수면 감지: IDLE + OnBody 때만 */
            sleepDetector.setEnabled(!isActive && isOnBody)

            recompute()
        }.launchIn(scope)

    /* (4) Dynamic */
    private var latestDynamic: DynamicType? = null
    private fun collectDynamic() = dynamicCls.state
        .onEach { latestDynamic = it; recompute() }
        .launchIn(scope)

    /* (5) Static */
    private var latestStatic: StaticType? = null
    private fun collectStatic() = staticCls.state          // ⭐
        .onEach { latestStatic = it; recompute() }
        .launchIn(scope)

    /* (6) PhoneUsage (선택) */
//    private var isPhoneUsing: Boolean = false              // ⭐
//    private fun collectPhoneUsage() {
//        phoneUsage?.onEach {
//            isPhoneUsing = it.isUsing
//            recompute()
//        }?.launchIn(scope)
//    }

    /* ───────── 최종 상태 합성 ───────── */
    private fun recompute() {
        val newState: ActivityState = when {
            !isOnBody                     -> ActivityState.OffBody
            sleepDetector.state.value is ActivityState.Sleeping
                                        -> ActivityState.Sleeping

            /* IDLE → StaticClassifier 결과 사용 */
            idleActiveDetector.state.value == SmaIdleActiveDetector.State.IDLE -> {
                latestStatic?.let {
                    ActivityState.Static(it)      // ⭐
//                    ActivityState.Static(it, isPhoneUsing)      // ⭐
                } ?: ActivityState.Idle                         // Static 미확정 시
            }

            /* ACTIVE → Dynamic 결과 우선 */
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