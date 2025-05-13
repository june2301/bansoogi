package com.ddc.bansoogi.activity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

/**
 * Consumes step-detector timestamps, pressure, linear acceleration, and heart rate,
 * and emits [DynamicType] based on various metrics.
 */
class DynamicClassifier(
    private val stepTimestamps: Flow<Long>,
    private val pressure: Flow<FloatArray>,
    private val linearAcceleration: Flow<FloatArray>,
    private val heartRate: Flow<Float>,
    externalScope: CoroutineScope? = null
) {

    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<DynamicType?>(null)
    val state: StateFlow<DynamicType?> = _state.asStateFlow()

    private val stepDeque60s = ArrayDeque<Long>()
    private val stepDeque6s = ArrayDeque<Long>()

    private val pressureDeque6s = ArrayDeque<Pair<Long, Float>>()

    private val smaBuffer = ArrayDeque<Float>()
    private val smaMaxSamples = 5 * 50 * 3

    private var currentHeartRate: Float = 0f

    init {
        collectSteps()
        collectPressure()
        collectLinAcc()
        collectHeartRate()
    }

    private fun collectSteps() {
        stepTimestamps.onEach { ts ->
            stepDeque60s.add(ts)
            val cutoff60 = ts - TimeUnit.SECONDS.toMillis(60)
            while (stepDeque60s.isNotEmpty() && stepDeque60s.first < cutoff60) stepDeque60s.removeFirst()

            stepDeque6s.add(ts)
            val cutoff6 = ts - TimeUnit.SECONDS.toMillis(6)
            while (stepDeque6s.isNotEmpty() && stepDeque6s.first < cutoff6) stepDeque6s.removeFirst()

            evaluate()
        }.launchIn(scope)
    }

    private fun collectPressure() {
        pressure.onEach { values ->
            val ts = System.currentTimeMillis()
            val hPa = values.firstOrNull() ?: return@onEach
            pressureDeque6s.add(ts to hPa)
            val cutoff6 = ts - TimeUnit.SECONDS.toMillis(6)
            while (pressureDeque6s.isNotEmpty() && pressureDeque6s.first().first < cutoff6) pressureDeque6s.removeFirst()

            evaluate()
        }.launchIn(scope)
    }

    private fun collectLinAcc() {
        linearAcceleration.onEach { values ->
            smaBuffer.add(values[0])
            smaBuffer.add(values[1])
            smaBuffer.add(values[2])
            while (smaBuffer.size > smaMaxSamples) smaBuffer.removeFirst()

            evaluate()
        }.launchIn(scope)
    }

    private fun collectHeartRate() {
        heartRate.onEach { bpm ->
            currentHeartRate = bpm
            evaluate()
        }.launchIn(scope)
    }

    private fun evaluate() {
        if (stepDeque60s.isEmpty()) return

        val cadenceSpm: Double = stepDeque60s.size * 60.0 / 60

        val stepCount6s = stepDeque6s.size
        val altDiff6s = computeAltitudeDiff6s()

        val sma: Float = if (smaBuffer.size == smaMaxSamples) {
            SmaFallbackClassifier.computeSma(smaBuffer.toFloatArray())
        } else 0f

        val newType = when {
            cadenceSpm >= 150 -> DynamicType.RUNNING
            altDiff6s > 0.5 && stepCount6s >= 3 -> DynamicType.CLIMBING
            cadenceSpm < 60 && sma > 2.5f && currentHeartRate > 100 -> DynamicType.EXERCISING
            else -> DynamicType.WALKING
        }

        if (_state.value != newType) {
            _state.value = newType
        }
    }

    private fun computeAltitudeDiff6s(): Double {
        if (pressureDeque6s.size < 2) return 0.0
        val oldest = pressureDeque6s.first().second
        val newest = pressureDeque6s.last().second
        val altOld = hPaToAltitude(oldest.toDouble())
        val altNew = hPaToAltitude(newest.toDouble())
        return altNew - altOld
    }

    private fun hPaToAltitude(hPa: Double): Double {
        return 44330.0 * (1.0 - pow(hPa / 1013.25, 0.190295))
    }
}