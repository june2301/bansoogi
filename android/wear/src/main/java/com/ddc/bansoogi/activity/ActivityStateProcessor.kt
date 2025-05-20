package com.ddc.bansoogi.activity

import android.util.Log
import com.ddc.bansoogi.common.mobile.communication.sender.BansoogiStateSender
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import com.ddc.bansoogi.sensor.AndroidSensorManager
import com.ddc.bansoogi.state.ProlongedStaticMonitor
import com.ddc.bansoogi.state.StaticBreakRewardMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/* ─────────────── 최종 ActivityState 모델 ─────────────── */
sealed class ActivityState {
    object OffBody                  : ActivityState()
    object Sleeping                 : ActivityState()
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
    /** 장시간 정적 상태 경고 기준(분). Wear 단말에 저장해 두었다가 주입 */
    private val notificationDurationMin: Int = 1,
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
        collectPhoneUsage()
    }

    fun stop() {
        sensorManager.stopAll()
        staticCls.stop()              // ⭐
        dynamicCls.stop()
        idleActiveDetector.stop()
        sleepDetector.stop()
        scope.cancel()
    }

    private fun collectOffBody() = sensorManager.isOffBody
        .onEach { isOnBody = it
            dynamicCls.setEnabled(isOnBody && isActive)
            staticCls.setEnabled(isOnBody && !isActive)
            sleepDetector.setEnabled(!isActive && isOnBody)

            if (!isOnBody) {
                Log.i(TAG, "🔌 OffBody detected → Stopping all sensors")
                sensorManager.stopAll()            // ✅ 센서 중지
                onStaticInterrupted()
            } else {
                Log.i(TAG, "⚡ OnBody detected → Restarting all sensors")
                sensorManager.startAll()           // ✅ 센서 재시작
            }

            recompute()
        }.launchIn(scope)

    private fun collectSleep() = sleepDetector.state
        .onEach { recompute() }
        .launchIn(scope)

    private var isActive = false
    private fun collectIdleActive() = idleActiveDetector.state
        .onEach { state ->
            val wasActive = isActive
            isActive = (state == SmaIdleActiveDetector.State.ACTIVE)
            if (wasActive && !isActive) onStaticInterrupted() // ACTIVE→IDLE 전이 시 초기화
            dynamicCls.setEnabled(isActive)
            staticCls.setEnabled(!isActive)
            sleepDetector.setEnabled(!isActive && isOnBody)
            recompute()
        }.launchIn(scope)

    private var latestDynamic: DynamicType? = null
    private fun collectDynamic() = dynamicCls.state
        .onEach { latestDynamic = it; recompute() }
        .launchIn(scope)

    /* (5) Static */
    /* ───────── Static 수집 로직 수정 ───────── */
    private var latestStatic: StaticType? = null
    private fun collectStatic() = staticCls.state
        .onEach { newStatic ->
            val currSitting = newStatic == StaticType.SITTING
            val currLying   = newStatic == StaticType.LYING

            // 1) ProlongedMonitor 입력
            prolongedMonitor.onStatic(currSitting, currLying)
            // 2) RewardMonitor 평가 (이전↔현재 비교)
            rewardMonitor.evaluate(prevSitting, prevLying, currSitting, currLying)
            // 3) 플래그 최신화
            prevSitting = currSitting
            prevLying   = currLying
            // 4) 상태 저장 및 재계산
            latestStatic = newStatic
            recompute()
        }
        .launchIn(scope)

    /* Idle→Active 또는 OffBody 로 전환될 때 Static 버퍼 초기화 */
    private fun onStaticInterrupted() {
        prolongedMonitor.onNonStatic()
        prevSitting = false; prevLying = false
    }

    /* (6) PhoneUsage (선택) */
    private var isPhoneUsing: Boolean = false              // ⭐
    private fun collectPhoneUsage() {
        phoneUsage?.onEach {
            val wasUsing = isPhoneUsing
            isPhoneUsing = it.isUsing

            // phoneUsage 변경 시 recompute 실행
            if (wasUsing != isPhoneUsing) {
                recompute()
            }
        }?.launchIn(scope)
    }

    private val prolongedMonitor = ProlongedStaticMonitor(
        ctx = sensorManager.context,
        scope = scope,
        notificationDurationMin = notificationDurationMin
    )
    private val rewardMonitor = StaticBreakRewardMonitor(
        ctx = sensorManager.context,
        scope = scope,
        prolonged = prolongedMonitor
    )

    // 상태 캐싱용 플래그
    private var prevSitting = false
    private var prevLying   = false

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
                }
                    ?: ActivityState.Static(StaticType.UNKNOWN)                         // Static 미확정 시
            }

            /* ACTIVE → Dynamic 결과 우선 */
            latestDynamic != null         -> ActivityState.Dynamic(latestDynamic!!)

            else                          -> ActivityState.Dynamic(DynamicType.UNKNOWN)
        }
        if (_state.value != newState) {
            _state.value = newState
            Log.d(TAG, "ActivityState → $newState")

            // ⭐ BansoogiState 업데이트 로직 추가
            val bansoogiState = newState.toBansoogiState(isPhoneUsing)
            CoroutineScope(Dispatchers.Main).launch {
                BansoogiStateHolder.updateWithMobile(sensorManager.context, bansoogiState)
                BansoogiStateSender.send(sensorManager.context, bansoogiState)
            }
        }
    }

    private companion object { const val TAG = "ActivityProcessor" }

    fun ActivityState.toBansoogiState(phoneUsage: Boolean): BansoogiState = when (this) {
        is ActivityState.Static -> {
            if (phoneUsage) BansoogiState.PHONE else when (type) {
                StaticType.SITTING, StaticType.LYING  -> BansoogiState.LIE
                StaticType.STANDING, StaticType.UNKNOWN -> BansoogiState.BASIC
            }
        }
        is ActivityState.Dynamic -> when (type) {
            DynamicType.WALKING     -> BansoogiState.WALK
            DynamicType.RUNNING     -> BansoogiState.RUN
            DynamicType.CLIMBING,
            DynamicType.EXERCISING  -> BansoogiState.RUN
            DynamicType.UNKNOWN     -> BansoogiState.BASIC
        }
        ActivityState.Sleeping      -> BansoogiState.SLEEP
        ActivityState.OffBody       -> BansoogiState.BASIC
        else                        -> BansoogiState.BASIC
    }
}