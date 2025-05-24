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

     enum class DynamicType { WALKING, RUNNING, CLIMBING, EXERCISING, UNKNOWN }

     /** ACTIVE 상태에서만 켜지며, EXERCISING 계산에만 SMA 사용 */
     class DynamicClassifier(
         private val stepTimestamps: Flow<Long>,
         private val pressure: Flow<FloatArray>,
         private val linearAcceleration: Flow<FloatArray>,
         private val heartRate: Flow<Float>,
         externalScope: CoroutineScope? = null,
         private val hz: Int = 50
     ) {
         /* ───────── 상수(Threshold) ───────── */
         companion object {
             private const val TAG = "DynamicClassifier"

             // 걷기·뛰기 구분용 스텝 속도
             private const val RUNNING_SPM_THRESHOLD = 150.0               // spm
             private const val WALKING_SPM_MIN       = 10.0
             private const val WALKING_SPM_MAX       = 149.99

             // EXERCISING 판별(스텝 無)
             private const val EXERCISE_SMA_THRESHOLD = 2.94f              // ≈0.30 g
             private const val EXERCISE_HR_THRESHOLD  = 100f               // bpm

             // CLIMBING 조건
             private const val CLIMB_ALT_THRESHOLD_M  = 0.9                // Δalt over 6 s
             private const val CLIMB_MIN_STEPS_6S     = 3                  // 최소 스텝
             private const val CLIMB_HOLD_MS          = 3_000L             // 3 s hold
         }

         /* ───────── 내부 상태 ───────── */
         private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
         private val _state = MutableStateFlow<DynamicType?>(null)
         val state: StateFlow<DynamicType?> = _state.asStateFlow()

         private var enabled = true
         fun setEnabled(v: Boolean) { enabled = v }
         fun stop() = scope.cancel()

         // 슬라이딩 윈도우 버퍼
         private val stepDeque60s = ArrayDeque<Long>()                    // 60 s 스텝
         private val stepDeque6s  = ArrayDeque<Long>()                    // 6 s 스텝
         private val pressDeque   = ArrayDeque<Pair<Long, Float>>()       // 6 s 압력
         private val smaBuf       = ArrayDeque<Float>()                   // 가속도 버퍼

         private val smaMax = 5 * hz * 3                                  // 3축×5 s
         private val smaMin = (smaMax * 0.8).toInt()
         private var hr: Float = 0f

         // CLIMBING 유지용
         private var climbStartTs: Long = 0L
         private val lock = Any()

         init {
             collectSteps()
             collectPressure()
             collectLinAcc()
             collectHr()
         }

         /* ───────── Flow collectors ───────── */

         private fun collectSteps() = stepTimestamps
             .onEach { ts ->
                 synchronized(lock) {
                     stepDeque60s.add(ts); stepDeque6s.add(ts)
                     val cut60 = ts - TimeUnit.SECONDS.toMillis(60)
                     val cut6  = ts - TimeUnit.SECONDS.toMillis(6)
                     while (stepDeque60s.isNotEmpty() && stepDeque60s.first < cut60) stepDeque60s.removeFirst()
                     while (stepDeque6s .isNotEmpty() && stepDeque6s .first < cut6 ) stepDeque6s .removeFirst()
                     evaluateLocked()
                 }
             }.launchIn(scope)

         private fun collectPressure() = pressure
             .onEach { v ->
                 val ts = System.currentTimeMillis()
                 val hPa = v.firstOrNull() ?: return@onEach
                 synchronized(lock) {
                     pressDeque.add(ts to hPa)
                     val cut6 = ts - TimeUnit.SECONDS.toMillis(6)
                     while (pressDeque.isNotEmpty() && pressDeque.first().first < cut6) pressDeque.removeFirst()
                     evaluateLocked()
                 }
             }.launchIn(scope)

         private fun collectLinAcc() = linearAcceleration
             .onEach { v ->
                 synchronized(lock) {
                     smaBuf.addAll(listOf(v[0], v[1], v[2]))
                     while (smaBuf.size > smaMax) smaBuf.removeFirst()
                     evaluateLocked()
                 }
             }.launchIn(scope)

         private fun collectHr() = heartRate
             .onEach { bpm ->
                 synchronized(lock) { hr = bpm; evaluateLocked() }
             }.launchIn(scope)

         /* ───────── Core evaluator ───────── */

         /** lock 내부에서만 호출 */
         private fun evaluateLocked() {
             if (!enabled) return

             val now         = System.currentTimeMillis()
             val cadenceSpm  = stepDeque60s.size.toDouble()
             val step6       = stepDeque6s.size
             val altDiff     = computeAltDiff6sLocked()
             val sma         = if (smaBuf.size >= smaMin)
                 SmaFallbackClassifier.computeSma(smaBuf.toFloatArray()) else 0f

             // 1) CLIMBING 조건
             val climbCond = altDiff >= CLIMB_ALT_THRESHOLD_M && step6 >= CLIMB_MIN_STEPS_6S
             if (climbCond && _state.value != DynamicType.CLIMBING) {
                 climbStartTs = now
                 Log.d(TAG, "Climb condition met; hold timer started")
             }

             /* ---- 상태 후보 결정 ---- */
             var candidate: DynamicType? = when {
                 /* 1️⃣ CLIMBING – 최우선 (3초 유지) */
                 climbCond                                   -> DynamicType.CLIMBING

                 /* 2️⃣ RUNNING */
                 cadenceSpm >= RUNNING_SPM_THRESHOLD         -> DynamicType.RUNNING

                 /* 3️⃣ EXERCISING – 스텝 조건 제거 */
                 sma > EXERCISE_SMA_THRESHOLD && hr > EXERCISE_HR_THRESHOLD
                     -> DynamicType.EXERCISING

                 /* 4️⃣ WALKING */
                 cadenceSpm in WALKING_SPM_MIN..WALKING_SPM_MAX
                     -> DynamicType.WALKING

                 /* 5️⃣ 그 외 */
                 else                                         -> DynamicType.UNKNOWN
             }

             /* ---- CLIMBING 3초 hold ---- */
             if (_state.value == DynamicType.CLIMBING) {
                 val held = now - climbStartTs
                 if (held < CLIMB_HOLD_MS) {
                     candidate = DynamicType.CLIMBING
                 } else if (!climbCond) {
                     Log.d(TAG, "Climb hold expired (${held} ms) → 재평가")
                 }
             }

             /* ---- 상태 변경 처리 ---- */
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