     package com.ddc.bansoogi.activity

    import android.util.Log
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
    import java.lang.Math.pow
    import java.util.ArrayDeque
    import java.util.concurrent.TimeUnit

    enum class DynamicType { WALKING, RUNNING, CLIMBING, EXERCISING }

    /** ACTIVE 상태에서만 켜지며, EXERCISING 계산에만 SMA 사용 */
    class DynamicClassifier(
        private val stepTimestamps: Flow<Long>,
        private val pressure: Flow<FloatArray>,
        private val linearAcceleration: Flow<FloatArray>,
        private val heartRate: Flow<Float>,
        externalScope: CoroutineScope? = null,
        private val hz: Int = 50
    ) {
        companion object {
            private const val TAG = "DynamicClassifier"
            private const val THRESHOLD_SMA   = 2.94f    // 0.30 g
            private const val CLIMB_THRESHOLD = 0.9      // meters over 6s
            private const val CLIMB_HOLD_MS   = 3_000L   // 3 seconds hold
        }

        private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val _state = MutableStateFlow<DynamicType?>(null)
        val state: StateFlow<DynamicType?> = _state.asStateFlow()

        private var enabled = true
        fun setEnabled(v: Boolean) { enabled = v }
        fun stop() = scope.cancel()

        // 슬라이딩 윈도우용 buffers
        private val stepDeque60s = ArrayDeque<Long>()
        private val stepDeque6s  = ArrayDeque<Long>()
        private val pressDeque   = ArrayDeque<Pair<Long, Float>>()
        private val smaBuf       = ArrayDeque<Float>()

        private val smaMax = 5 * hz * 3
        private val smaMin = (smaMax * 0.8).toInt()
        private var hr: Float = 0f

        // 클라이밍 상태 진입 타임스탬프
        private var climbStartTs: Long = 0L

        private val lock = Any()

        init {
            collectSteps()
            collectPressure()
            collectLinAcc()
            collectHr()
        }

        private fun collectSteps() = stepTimestamps.onEach { ts ->
            synchronized(lock) {
                stepDeque60s.add(ts)
                stepDeque6s.add(ts)
                val cut60 = ts - TimeUnit.SECONDS.toMillis(60)
                val cut6  = ts - TimeUnit.SECONDS.toMillis(6)
                while (stepDeque60s.isNotEmpty() && stepDeque60s.first < cut60) stepDeque60s.removeFirst()
                while (stepDeque6s .isNotEmpty() && stepDeque6s .first < cut6 ) stepDeque6s .removeFirst()
                evaluateLocked()
            }
        }.launchIn(scope)

        private fun collectPressure() = pressure.onEach { v ->
            val ts = System.currentTimeMillis()
            val hPa = v.firstOrNull() ?: return@onEach
            synchronized(lock) {
                pressDeque.add(ts to hPa)
                val cut6 = ts - TimeUnit.SECONDS.toMillis(6)
                while (pressDeque.isNotEmpty() && pressDeque.first().first < cut6) pressDeque.removeFirst()
                evaluateLocked()
            }
        }.launchIn(scope)

        private fun collectLinAcc() = linearAcceleration.onEach { v ->
            synchronized(lock) {
                smaBuf.addAll(listOf(v[0], v[1], v[2]))
                while (smaBuf.size > smaMax) smaBuf.removeFirst()
                evaluateLocked()
            }
        }.launchIn(scope)

        private fun collectHr() = heartRate.onEach { bpm ->
            synchronized(lock) {
                hr = bpm
                evaluateLocked()
            }
        }.launchIn(scope)

        /** lock 내부에서만 호출 */
        private fun evaluateLocked() {
            if (!enabled) return

            val now = System.currentTimeMillis()
            val cadenceSpm = stepDeque60s.size.toDouble()
            val step6      = stepDeque6s.size
            val altDiff    = computeAltDiff6sLocked()
            val sma        = if (smaBuf.size >= smaMin)
                SmaFallbackClassifier.computeSma(smaBuf.toFloatArray())
            else 0f

            // CLIMBING 조건 판정
            val climbCond = altDiff >= CLIMB_THRESHOLD && step6 >= 3

            // 즉시 진입: 조건 만족 시 타임스탬프 설정
            if (climbCond && _state.value != DynamicType.CLIMBING) {
                climbStartTs = now
                Log.d(TAG, "Climb condition met; start hold at $climbStartTs")
            }

            // 상태 후보 결정 (기본 로직)
            var candidate: DynamicType? = when {
                cadenceSpm >= 150                            -> DynamicType.RUNNING
                climbCond                                   -> DynamicType.CLIMBING
                cadenceSpm in 10.0..<60.0 && sma > THRESHOLD_SMA && hr > 100
                    -> DynamicType.EXERCISING
                cadenceSpm in 10.0..<150.0                   -> DynamicType.WALKING
                else                                         -> null
            }

            // 3초 유지 로직: CLIMBING 상태에서만 강제 유지
            if (_state.value == DynamicType.CLIMBING) {
                val held = now - climbStartTs
                if (held < CLIMB_HOLD_MS) {
                    candidate = DynamicType.CLIMBING
                } else if (!climbCond) {
                    // 3초 지났고 조건도 깨졌다면 내려옴
                    Log.d(TAG, "Climb hold expired (${held}ms), exiting CLIMBING")
                    // candidate remains whatever was computed above
                }
            }

            // 상태 변경 시 로그 & 업데이트
            if (_state.value != candidate) {
                Log.d(TAG, "DynamicType → $candidate")
                _state.value = candidate
            }
        }

        /** lock 내에서만 호출 */
        private fun computeAltDiff6sLocked(): Double {
            if (pressDeque.size < 2) return 0.0
            val hOld = pressDeque.peekFirst()!!.second
            val hNew = pressDeque.peekLast()!!.second
            return hPaToAlt(hNew) - hPaToAlt(hOld)
        }

        private fun hPaToAlt(h: Float) =
            44330.0 * (1.0 - pow((h / 1013.25).toDouble(), 0.190295))
    }