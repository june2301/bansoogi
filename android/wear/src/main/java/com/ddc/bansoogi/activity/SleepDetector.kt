package com.ddc.bansoogi.activity

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit

/**
 * 수면(ASLEEP) 상태 감지기
 * - 착용 중이며 Idle 상태일 때만 enable=true 로 두도록 외부에서 제어한다.
 * - 진입 조건  : SMA < 0.25 && HR < 55  가 3분 연속 유지될 때
 * - 진출 조건  : (SMA > 0.5 || HR > 55) 가 3분 연속 유지될 때
 *                단, SMA > 0.5 && HR > 55 둘 다 만족하면 타이머를 절반(1.5분)으로 단축한다.
 */
class SleepDetector(
    linAcc: Flow<FloatArray>,
    heartRate: Flow<Float>,
    externalScope: CoroutineScope? = null,
    private val hz: Int = 50
) {
    companion object {
        private const val TAG = "SleepDetector"

        // ───────── 진입/진출 임계값 ─────────
        private const val ENTRY_SMA_MAX = 0.25f      // m/s²
        private const val ENTRY_HR_MAX  = 55f        // bpm
        private const val EXIT_SMA_MIN  = 0.5f       // m/s²
        private const val EXIT_HR_MIN   = 55f        // bpm (동일)

        private const val ENTRY_HOLD_MS = 3 * 60 * 1000L  // 3분
        private const val EXIT_HOLD_MS  = 3 * 60 * 1000L  // 3분 (기본)
    }

    // ───────── 외부 제어 ─────────
    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var enabled = false
    fun setEnabled(v: Boolean) { enabled = v }
    fun stop() = scope.cancel()

    // ───────── 내부 버퍼 ─────────
    private val smaBuf = ArrayDeque<Float>()
    private val smaMax = 5 * hz * 3         // 3축 × 5 s
    private val smaMin = (smaMax * 0.8).toInt()
    private var hr: Float = 60f

    private val lock = Any()

    // 타이머
    private var entryStartTs = 0L
    private var exitStartTs  = 0L

    // ───────── 상태 플로우 ─────────
    private val _state = MutableStateFlow<ActivityState?>(null) // Sleeping 또는 null
    val state: StateFlow<ActivityState?> = _state.asStateFlow()

    init {
        collectLinAcc(linAcc)
        collectHr(heartRate)
    }

    private fun collectLinAcc(src: Flow<FloatArray>) = src
        .onEach { v ->
            synchronized(lock) {
                smaBuf.addAll(listOf(v[0], v[1], v[2]))
                while (smaBuf.size > smaMax) smaBuf.removeFirst()
                evaluateLocked()
            }
        }.launchIn(scope)

    private fun collectHr(src: Flow<Float>) = src
        .onEach { bpm ->
            synchronized(lock) {
                hr = bpm
                evaluateLocked()
            }
        }.launchIn(scope)

    /** lock 내부에서만 호출 */
    private fun evaluateLocked() {
        if (!enabled) {
            // detector disabled → 상태 초기화
            if (_state.value != null) {
                _state.value = null
                entryStartTs = 0L; exitStartTs = 0L
            }
            return
        }

        val now = System.currentTimeMillis()
        val sma = if (smaBuf.size >= smaMin) {
            SmaFallbackClassifier.computeSma(smaBuf.toFloatArray())
        } else 0f

        val isSleeping = _state.value is ActivityState.Sleeping
        val entryCond  = sma < ENTRY_SMA_MAX && hr < ENTRY_HR_MAX
        val exitSma    = sma > EXIT_SMA_MIN
        val exitHr     = hr > EXIT_HR_MIN
        val anyExit    = exitSma || exitHr
        val bothExit   = exitSma && exitHr
        val effectiveExitHold = if (bothExit) EXIT_HOLD_MS / 2 else EXIT_HOLD_MS

        // ───── 수면 진입 로직 ─────
        if (!isSleeping) {
            if (entryCond) {
                if (entryStartTs == 0L) entryStartTs = now
                val held = now - entryStartTs
                if (held >= ENTRY_HOLD_MS) {
                    Log.d(TAG, "Sleep entry after ${held} ms | SMA=$sma, HR=$hr")
                    _state.value = ActivityState.Sleeping
                    entryStartTs = 0L
                }
            } else {
                entryStartTs = 0L
            }
            // 아직 수면 아님 → 종료 로직 패스
            return
        }

        // ───── 수면 진출 로직 (현재 Sleep 상태) ─────
        if (anyExit) {
            if (exitStartTs == 0L) exitStartTs = now
            val held = now - exitStartTs
            if (held >= effectiveExitHold) {
                Log.d(TAG, "Sleep exit after ${held} ms | SMA=$sma, HR=$hr, both=$bothExit")
                _state.value = null // sleeping 해제
                exitStartTs = 0L
            }
        } else {
            exitStartTs = 0L
        }
    }
}
