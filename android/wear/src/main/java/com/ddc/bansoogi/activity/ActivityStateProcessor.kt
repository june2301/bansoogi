package com.ddc.bansoogi.activity

import android.util.Log
import com.ddc.bansoogi.common.mobile.communication.sender.BansoogiStateSender
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import com.ddc.bansoogi.sensor.AndroidSensorManager
import com.ddc.bansoogi.state.ProlongedStaticMonitor
//import com.ddc.bansoogi.state.ProlongedStaticMonitorHolder
import com.ddc.bansoogi.state.StaticBreakRewardMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/* ─────────────── ActivityState 모델 ─────────────── */
sealed class ActivityState {
    object OffBody : ActivityState()
    object Sleeping : ActivityState()
    data class Static(val type: StaticType) : ActivityState()
    data class Dynamic(val type: DynamicType) : ActivityState()
    object Unknown : ActivityState()
}

/* ─────────────── PHONE_USAGE DTO ─────────────── */
data class PhoneUsageDto(val isUsing: Boolean)

/**
 * 센서 → 상태 → 알림/애니메이션 파이프라인을 담당.
 * 수정사항
 *   ① Off‑body·IDLE→ACTIVE 전이에 pending 을 지우지 않음 (onNonStaticWindowReset())
 *   ② rewardMonitor.tick() 를 매 프레임 호출 & WARN 콜백 한 번만 전달
 */
class ActivityStateProcessor(
    private val sensorManager: AndroidSensorManager,
    private val phoneUsage: Flow<PhoneUsageDto>? = null,
    externalScope: CoroutineScope? = null
) {
    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /* ───── 모니터 */
    private val prolongedMonitor = ProlongedStaticMonitor(sensorManager.context, scope)
    private val rewardMonitor    = StaticBreakRewardMonitor(sensorManager.context, scope, prolongedMonitor)

    /* ───── 디텍터/분류기 */
    private val idleActiveDetector = SmaIdleActiveDetector(
        linAcc = sensorManager.linearAcceleration,
        stepTimestamps = sensorManager.stepDetector,
        externalScope = scope
    )
    private val staticCls = StaticClassifier(
        linearAcceleration = sensorManager.linearAcceleration,
        heartRate          = sensorManager.heartRate,
        ppgGreen           = sensorManager.ppgGreen,
        externalScope      = scope
    )
    private val dynamicCls = DynamicClassifier(
        stepTimestamps     = sensorManager.stepDetector,
        pressure           = sensorManager.pressure,
        linearAcceleration = sensorManager.linearAcceleration,
        heartRate          = sensorManager.heartRate,
        externalScope      = scope
    )
    private val sleepDetector = SleepDetector(
        linAcc       = sensorManager.linearAcceleration,
        heartRate    = sensorManager.heartRate,
        externalScope = scope
    )

    /* ───── 상태 Flow */
    private val _state = MutableStateFlow<ActivityState>(ActivityState.Unknown)
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    /* ───── 내부 플래그 */
    private var isOnBody = true
    private var isActive = false
    private var latestStatic: StaticType? = null
    private var latestDynamic: DynamicType? = null
    private var isPhoneUsing = false
    private var staticTickerJob: Job? = null

    /* ───── Lifecycle */
    fun start() {
        sensorManager.startAll()
        collectOffBody()
        collectIdleActive()
        collectStatic()
        collectDynamic()
        collectSleep()
        collectPhoneUsage()
        collectStepEvents()
        startAccumTicker()                         // ✨
        startStaticTicker()

        prolongedMonitor.rewardCallback = { rewardMonitor.onWarnIssued(it) }

//        ProlongedStaticMonitorHolder.monitor = prolongedMonitor
    }

    fun stop() {
        sensorManager.stopAll()
        staticTickerJob?.cancel()
        staticCls.stop(); dynamicCls.stop(); idleActiveDetector.stop(); sleepDetector.stop()
        scope.cancel()
    }

    /* ───── Sensor collectors ───── */
    private fun collectStepEvents() {
        sensorManager.stepDetector
            .onEach {
                rewardMonitor.onStepDetected()
                rewardMonitor.tick(isStatic = false, isActive = true)   // ★ 추가
            }
            .launchIn(scope)
    }

    /* ─── 5분 주기 누계 & 창 업데이트 ─── */
    private fun startStaticTicker() {
        staticTickerJob?.cancel()
        staticTickerJob = scope.launch {
            while (scope.isActive) {
                // 1) 누계 타이머
                rewardMonitor.onStaticFrame(latestStatic)

                // 2) 슬라이딩 윈도우 커버리지 보강
                prolongedMonitor.onStatic(
                    isSitting = latestStatic == StaticType.SITTING,
                    isLying   = latestStatic == StaticType.LYING
                )

                delay(1_000L)
            }
        }
    }

    private fun collectOffBody() = sensorManager.isOffBody
        .onEach { offBody ->
            isOnBody = offBody
            dynamicCls.setEnabled(isOnBody && isActive)
            staticCls.setEnabled(isOnBody && !isActive)
            sleepDetector.setEnabled(!isActive && isOnBody)

            if (!offBody) {
                Log.i(TAG, "🔌 Off‑Body → stop sensors")
                sensorManager.stopAll()
                stopStaticTicker()
                prolongedMonitor.onNonStaticWindowReset() // ✔ pending 유지
            } else {
                Log.i(TAG, "⚡ On‑Body → restart sensors")
                sensorManager.startAll()
                startStaticTicker()
            }
            recompute()
        }.launchIn(scope)

    private fun stopStaticTicker() {
        staticTickerJob?.cancel()
        staticTickerJob = null
    }

    private fun collectIdleActive() = idleActiveDetector.state
        .onEach { s ->
            isActive = (s == SmaIdleActiveDetector.State.ACTIVE)
            dynamicCls.setEnabled(isActive)
            staticCls.setEnabled(!isActive)
            sleepDetector.setEnabled(!isActive && isOnBody)
            rewardMonitor.tick(
                isStatic = isActive.not() &&
                        (latestStatic == StaticType.SITTING || latestStatic == StaticType.LYING),
                isActive = isActive
            )
            recompute()
        }.launchIn(scope)

    private fun collectStatic() = staticCls.state
        .map { _ -> StaticType.STANDING }
        .onEach { st ->
            latestStatic = st
            val isStatic = st == StaticType.SITTING || st == StaticType.LYING

            prolongedMonitor.onStatic(st == StaticType.SITTING, st == StaticType.LYING)
            rewardMonitor.tick(isStatic = isStatic, isActive = isActive)

            /* ★ 누적 타이머 업데이트 */
            rewardMonitor.onStaticFrame(st)

            recompute()
        }.launchIn(scope)

    private fun collectDynamic() = dynamicCls.state
        .onEach {
            latestDynamic = it
            rewardMonitor.tick(isStatic = false, isActive = true)   // ★ 추가
            recompute()
        }
        .launchIn(scope)

    private fun collectSleep() = sleepDetector.state
        .onEach { recompute() }
        .launchIn(scope)

    private fun collectPhoneUsage() {
        phoneUsage?.onEach {
            val prev = isPhoneUsing; isPhoneUsing = it.isUsing
            if (prev != isPhoneUsing) recompute()
        }?.launchIn(scope)
    }

    private fun startAccumTicker() {
        scope.launch {
            while (isActive.not() && scope.isActive) {
                rewardMonitor.onStaticFrame(latestStatic)
                delay(1_000L)                      // ✨ 1초마다 누계
            }
        }
    }

    /* ───── 합성 & 전송 ───── */
    private fun recompute() {
        val newState: ActivityState = when {
            !isOnBody -> ActivityState.OffBody
            sleepDetector.state.value is ActivityState.Sleeping -> ActivityState.Sleeping
            idleActiveDetector.state.value == SmaIdleActiveDetector.State.IDLE ->
                latestStatic?.let { ActivityState.Static(it) } ?: ActivityState.Static(StaticType.UNKNOWN)
            latestDynamic != null -> ActivityState.Dynamic(latestDynamic!!)
            else -> ActivityState.Dynamic(DynamicType.UNKNOWN)
        }

        if (_state.value != newState) {
            _state.value = newState
            Log.d(TAG, "ActivityState → $newState")
            val bs = newState.toBansoogiState(isPhoneUsing)
            CoroutineScope(Dispatchers.Main).launch {
                BansoogiStateHolder.updateWithMobile(sensorManager.context, bs)
//                BansoogiStateSender.send(sensorManager.context, bs)
            }
        }
    }

    /* ───── 매핑 ───── */
    private fun ActivityState.toBansoogiState(phoneUse: Boolean): BansoogiState = when (this) {
        is ActivityState.Static -> if (phoneUse) BansoogiState.PHONE else when (type) {
            StaticType.SITTING, StaticType.LYING -> BansoogiState.LIE
            StaticType.STANDING, StaticType.UNKNOWN -> BansoogiState.BASIC
        }
        is ActivityState.Dynamic -> when (type) {
            DynamicType.WALKING -> BansoogiState.WALK
            DynamicType.RUNNING -> BansoogiState.RUN
            DynamicType.CLIMBING, DynamicType.EXERCISING -> BansoogiState.RUN
            DynamicType.UNKNOWN -> BansoogiState.BASIC
        }
        ActivityState.Sleeping -> BansoogiState.SLEEP
        ActivityState.OffBody  -> BansoogiState.BASIC
        else -> BansoogiState.BASIC
    }

    private companion object { const val TAG = "ActivityProcessor" }
}
