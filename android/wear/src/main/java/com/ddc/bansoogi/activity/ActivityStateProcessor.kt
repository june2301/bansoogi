package com.ddc.bansoogi.activity

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.ArrayDeque

/**
 * Collects raw sensor events from [com.ddc.bansoogi.sensor.AndroidSensorManager] and pipelines them to
 * higher-level classifiers (to be implemented). For now it simply logs events
 * so that we can verify the data→분류 흐름의 첫 구간이 살아 있는지 빠르게 확인한다.
 */
class ActivityStateProcessor(
    private val sensorManager: com.ddc.bansoogi.sensor.AndroidSensorManager,
    externalScope: CoroutineScope? = null
) {

    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        sensorManager.startAll()
        collectSensors()
    }

    fun stop() {
        sensorManager.stopAll()
        scope.cancel()
    }

    private fun collectSensors() {
        // Off-body
        sensorManager.offBody
            .onEach { onBody -> Log.d(TAG, "OffBody onBody=$onBody") }
            .launchIn(scope)

        // Constants for SMA fallback
        val windowSeconds = 5
        val linAccHz = 50
        val maxSamples = windowSeconds * linAccHz
        val buffer = ArrayDeque<Float>(maxSamples * 3)
        var fallbackState: ActivityState = ActivityState.IDLE

        // Linear acceleration
        sensorManager.linearAcceleration
            .onEach { values ->
                // Append newest x,y,z
                buffer.add(values[0])
                buffer.add(values[1])
                buffer.add(values[2])
                // Trim to last N samples (x,y,z → *3)
                while (buffer.size > maxSamples * 3) {
                    buffer.removeFirst()
                }

                // Compute SMA when window full
                if (buffer.size == maxSamples * 3) {
                    val sma = SmaFallbackClassifier.computeSma(buffer.toFloatArray())
                    val newState = if (sma > 0.30f) ActivityState.ACTIVE else ActivityState.IDLE
                    if (newState != fallbackState) {
                        Log.d(TAG, "SMA fallback: $sma -> $newState")
                        fallbackState = newState
                        // TODO: integrate with system UserActivityState when available
                    }
                }
            }
            .launchIn(scope)

        // Step detector → cadence 계산 예정
        sensorManager.stepDetector
            .onEach { ts -> Log.d(TAG, "StepDetector t=$ts") }
            .launchIn(scope)

        // Pressure
        sensorManager.pressure
            .onEach { values -> Log.d(TAG, "Pressure hPa=${values.firstOrNull()} ") }
            .launchIn(scope)

        // Heart-rate
        sensorManager.heartRate
            .onEach { bpm -> Log.d(TAG, "HeartRate bpm=$bpm") }
            .launchIn(scope)

        // ───── Dynamic classifier ─────
        val dynamicClassifier = DynamicClassifier(
            stepTimestamps = sensorManager.stepDetector,
            pressure = sensorManager.pressure,
            linearAcceleration = sensorManager.linearAcceleration,
            heartRate = sensorManager.heartRate,
            externalScope = scope
        )

        dynamicClassifier.state
            .onEach { dyn -> dyn?.let { Log.d(TAG, "Dynamic → $it") } }
            .launchIn(scope)
    }

    companion object {
        private const val TAG = "ActivityProcessor"
    }
}