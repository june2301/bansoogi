package com.ddc.bansoogi.activity

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

        // Linear acceleration
        sensorManager.linearAcceleration
            .onEach { values -> Log.d(TAG, "LinAcc x=${values[0]} y=${values[1]} z=${values[2]}") }
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
    }

    companion object {
        private const val TAG = "ActivityProcessor"
    }
}