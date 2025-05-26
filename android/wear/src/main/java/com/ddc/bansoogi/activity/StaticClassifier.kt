package com.ddc.bansoogi.activity

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.ArrayDeque
import kotlin.math.acos
import kotlin.math.sqrt

enum class StaticType {
    LYING, SITTING, STANDING, UNKNOWN
}

class StaticClassifier(
    private val linearAcceleration: Flow<FloatArray>,
    private val heartRate: Flow<Float>,
    private val ppgGreen: Flow<Float>,                    // ✅ 추가됨
    externalScope: CoroutineScope? = null,
    private val hz: Int = 50
) {
    companion object {
        private const val TAG = "StaticClassifier"
        private const val WINDOW_SECONDS = 5
        private const val ENTRY_DELAY_MS = 7_000L          // ⭐ 10 초 진입 지연
    }

    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<StaticType?>(null)
    val state: StateFlow<StaticType?> = _state.asStateFlow()

    private var enabled = true
    fun setEnabled(v: Boolean) = synchronized(lock) {
        enabled = v
        resetStateMachine()
        Log.i(TAG, "Classifier ${if (v) "ENABLED" else "DISABLED"}")
    }
    fun stop() = scope.cancel()

    /* ────────────── 버퍼 ────────────── */
    private val linAccBuf = ArrayDeque<FloatArray>()
    private val hrBuf = ArrayDeque<Float>()
    private val ppgBuf = ArrayDeque<Float>()                  // ✅ PPG 버퍼 추가
    private val maxSamples = hz * WINDOW_SECONDS
    private val lock = Any()

    /* ────────────── 상태머신 변수 ────────────── */
    private var confirmedPosture   = StaticType.UNKNOWN  // 확정(출력) 상태
    private var candidatePosture   = StaticType.UNKNOWN  // 진입 대기중 후보
    private var candidateStartTime = 0L                  // 후보 시작 시각(ms)

    /* ────────────── 초기화 ────────────── */
    init {
        collectLinAcc()
        collectHr()
        collectPpgGreen()                                     // ✅ PPG 구독 시작
    }

    /* ────────────── 센서 수집 ────────────── */
    private fun collectLinAcc() = linearAcceleration
        .onEach { acc ->
            synchronized(lock) {
                linAccBuf.add(acc)
                while (linAccBuf.size > maxSamples) linAccBuf.removeFirst()
//                Log.d(TAG, "LIN_ACC  x=${acc[0]}  y=${acc[1]}  z=${acc[2]}")
                evaluateLocked()
            }
        }
        .launchIn(scope)

    private fun collectHr() = heartRate
        .onEach { bpm ->
            synchronized(lock) {
                hrBuf.add(bpm)
                while (hrBuf.size > maxSamples) hrBuf.removeFirst()
//                Log.d(TAG, "HR  bpm=$bpm")
                evaluateLocked()
            }
        }
        .launchIn(scope)

    private fun collectPpgGreen() = ppgGreen
        .onEach { value ->
            synchronized(lock) {
                ppgBuf.add(value)
                while (ppgBuf.size > maxSamples) ppgBuf.removeFirst()
//                Log.d(TAG, "PPG_GREEN  val=$value")
                evaluateLocked()
            }
        }
        .launchIn(scope)

    /* ────────────── 핵심 평가 ────────────── */
    private fun evaluateLocked() {
        if (!enabled || linAccBuf.isEmpty()) return

        /* 1️⃣ tilt 각도 계산 */
        val acc = linAccBuf.last()
        val (ax, ay, az) = acc.map(Float::toDouble)
        val mag = sqrt(ax*ax + ay*ay + az*az)
        if (mag < 1e-3) return
        val tiltDeg = Math.toDegrees(acos(az / mag))   // 0°=누움, 90°=세움
//        Log.d(TAG, "tilt=${"%.1f".format(tiltDeg)}°")                    // 분모 안전


        // 2️⃣ tilt 기준으로 후보 자세 산출
        val newCandidate: StaticType = when {
            tiltDeg < 25          -> StaticType.LYING
            tiltDeg < 65          -> StaticType.SITTING
            tiltDeg <= 120        -> StaticType.STANDING   // 팔 쭉 펴도 허용치
            else                  -> StaticType.UNKNOWN
        }

        // 3️⃣ 진입 딜레이 상태머신
        if (newCandidate == confirmedPosture) {
            // 이미 확정된 상태 유지 중 → 타이머·후보 리셋
            candidatePosture = StaticType.UNKNOWN
            candidateStartTime = 0L
            return
        }

        val now = System.currentTimeMillis()

        if (newCandidate != candidatePosture) {
            // 새 후보 등장 → 리셋 & 타이머 시작
            candidatePosture = newCandidate
            candidateStartTime = now
            Log.d(TAG, "▶ NEW CANDIDATE  $candidatePosture  (timer start)")
            return
        }

        // 동일 후보가 유지되는 중 → 지속 시간 체크
        val elapsed = now - candidateStartTime
//        Log.d(TAG, "⏳ CANDIDATE [$candidatePosture]  elapsed=${elapsed}ms")
        if (elapsed >= ENTRY_DELAY_MS) {
            confirmedPosture = candidatePosture
            _state.value = confirmedPosture
            Log.i(TAG, "✔ POSTURE CONFIRMED → $confirmedPosture")
            candidatePosture = StaticType.UNKNOWN
            candidateStartTime = 0L
        }
    }

    /* ────────────── 유틸 ────────────── */
    private fun resetStateMachine() {             // ⭐ start/stop 시 호출
        confirmedPosture   = StaticType.UNKNOWN
        candidatePosture   = StaticType.UNKNOWN
        candidateStartTime = 0L
        _state.value = null
        Log.d(TAG, "StateMachine RESET")
    }
}