package com.ddc.bansoogi.activity

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.ArrayDeque

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
    }

    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<StaticType?>(null)
    val state: StateFlow<StaticType?> = _state.asStateFlow()

    private var enabled = true
    fun setEnabled(v: Boolean) { enabled = v }
    fun stop() = scope.cancel()

    private val linAccBuf = ArrayDeque<FloatArray>()
    private val hrBuf = ArrayDeque<Float>()
    private val ppgBuf = ArrayDeque<Float>()                  // ✅ PPG 버퍼 추가
    private val maxSamples = hz * WINDOW_SECONDS
    private val lock = Any()

    init {
        collectLinAcc()
        collectHr()
        collectPpgGreen()                                     // ✅ PPG 구독 시작
    }

    private fun collectLinAcc() = linearAcceleration
        .onEach { acc ->
            synchronized(lock) {
                linAccBuf.add(acc)
                while (linAccBuf.size > maxSamples) linAccBuf.removeFirst()
                evaluateLocked()
            }
        }
        .launchIn(scope)

    private fun collectHr() = heartRate
        .onEach { bpm ->
            synchronized(lock) {
                hrBuf.add(bpm)
                while (hrBuf.size > maxSamples) hrBuf.removeFirst()
                evaluateLocked()
            }
        }
        .launchIn(scope)

    private fun collectPpgGreen() = ppgGreen
        .onEach { value ->
            synchronized(lock) {
                ppgBuf.add(value)
                while (ppgBuf.size > maxSamples) ppgBuf.removeFirst()
                evaluateLocked()
            }
        }
        .launchIn(scope)

    /** Core evaluation (lock 내부) */
    private fun evaluateLocked() {
        if (!enabled) return

        // TODO: 자세 인식 알고리즘 구현
        val candidate: StaticType = StaticType.UNKNOWN

        if (_state.value != candidate) {
            _state.value = candidate
            Log.d(TAG, "StaticType → $candidate")
        }
    }
}
