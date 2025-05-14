// SmaIdleActiveDetector.kt
package com.ddc.bansoogi.activity

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/** IDLE ↔ ACTIVE 를 구분하기 위한 5 s SMA 감시기 + 60 s 스텝 카운트 */
class SmaIdleActiveDetector(
    linAcc: Flow<FloatArray>,
    stepTimestamps: Flow<Long>,
    externalScope: CoroutineScope? = null,
    private val hz: Int = 50,                 // 가속도 샘플링 주파수(Hz)
    private val threshold: Float = 1.96f,     // 0.20 g (m/s²)
    private val stepThreshold: Int = 4       // 20s 창에서 최소 걸음 수
) {
    private val TAG = "SmaIdleActiveDetector"
    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // SMA 버퍼: 5 s × 1축
    private val windowSize = 5 * hz
    private val minReady  = (windowSize * 0.8).toInt()

    // 20 s 창의 스텝 타임스탬프
    private val stepWindowMs = TimeUnit.SECONDS.toMillis(20)
    private val stepDeque = ArrayDeque<Long>()

    private val buf = ArrayDeque<Float>()
    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    enum class State { IDLE, ACTIVE }

    init {
        // 1) 중력 제거된 선형 가속도 → 버퍼에
        linAcc.onEach { v ->
            val mag = sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2])
            val lin = kotlin.math.abs(mag - 9.81f)
            buf.add(lin)
            while (buf.size > windowSize) buf.removeFirst()
            evaluate()
        }.launchIn(scope)

        // 2) 스텝 이벤트 → 60s 창 관리
        stepTimestamps.onEach { ts ->
            stepDeque.add(ts)
            val cutoff = ts - stepWindowMs
            while (stepDeque.isNotEmpty() && stepDeque.first < cutoff) {
                stepDeque.removeFirst()
            }
            evaluate()
        }.launchIn(scope)
    }

    private var lastLogTs = 0L
    private fun evaluate() {
        // 버퍼 준비조건: SMA 버퍼가 차거나, 스텝 버퍼가 참고치 이상일 때
        if (buf.size < minReady && stepDeque.size < stepThreshold) return

        val now = System.currentTimeMillis()
        // SMA 계산
        val sma = SmaFallbackClassifier.computeSma(buf.toFloatArray())

        // 4초 주기 로그
        if (now - lastLogTs >= 4_000) {
            Log.d(TAG, "Periodic SMA=$sma | steps=${stepDeque.size}")
            lastLogTs = now
        }

        // ACTIVE 조건: SMA 초과 OR 60s 내 걸음 충분
        val newState = if (sma > threshold || stepDeque.size >= stepThreshold) {
            State.ACTIVE
        } else {
            State.IDLE
        }

        if (_state.value != newState) {
            Log.d(TAG, "SMA=$sma, steps=${stepDeque.size} → State=$newState")
            _state.value = newState
        }
    }

    fun stop() = scope.cancel()
}
