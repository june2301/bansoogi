package com.ddc.bansoogi.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * High-level façade that owns all Android-framework sensors used by the project.
 *
 * Only registration/un-registration + Kotlin `Flow` wiring is provided for now –
 * business-logic such as SMA, cadence, etc. lives in higher-level classes.
 */
class AndroidSensorManager(private val context: Context) {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Internal wrappers – not exposed outside this class
    private val offBodyWrapper by lazy { BooleanSensorWrapper(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT) }
    private val linAccWrapper by lazy { LinearAccelerationSensorWrapper() }
    private val stepDetectorWrapper by lazy { StepDetectorSensorWrapper() }
    private val pressureWrapper by lazy {
        FloatArraySensorWrapper(
            Sensor.TYPE_PRESSURE,
            samplingHz = 1
        )
    }
    private val heartRateWrapper by lazy { HeartRateSensorWrapper() }

    // Public read-only event streams
    val offBody = offBodyWrapper.events
    val linearAcceleration = linAccWrapper.events
    val stepDetector = stepDetectorWrapper.events
    val pressure = pressureWrapper.events
    val heartRate = heartRateWrapper.events

    fun startAll() {
        offBodyWrapper.start()
        linAccWrapper.start()
        stepDetectorWrapper.start()
        pressureWrapper.start()
        heartRateWrapper.start()
    }

    fun stopAll() {
        offBodyWrapper.stop()
        linAccWrapper.stop()
        stepDetectorWrapper.stop()
        pressureWrapper.stop()
        heartRateWrapper.stop()
    }

    // ========================================================================
    //  Generic base wrapper
    // ========================================================================

    private abstract inner class BaseSensorWrapper<T : Any>(
        private val sensorType: Int,
        private val samplingUs: Int
    ) : SensorEventListener {

        private val sensor: Sensor? = sensorManager.getDefaultSensor(sensorType)
        private val _events = MutableSharedFlow<T>(extraBufferCapacity = 64)
        val events: SharedFlow<T> = _events.asSharedFlow()

        fun start() {
            sensor?.let {
                sensorManager.registerListener(this, it, samplingUs)
            }
        }

        fun stop() {
            sensorManager.unregisterListener(this)
        }

        protected fun emit(value: T) {
            _events.tryEmit(value)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    // ========================================================================
    //  Concrete wrappers
    // ========================================================================

    /** TYPE_LINEAR_ACCELERATION @ 50 Hz */
    private inner class LinearAccelerationSensorWrapper :
        BaseSensorWrapper<FloatArray>(
            Sensor.TYPE_LINEAR_ACCELERATION,
            samplingUs = 1_000_000 / 50 // 50 Hz
        ) {
        override fun onSensorChanged(event: SensorEvent) {
            emit(event.values.clone())
        }
    }

    /** TYPE_STEP_DETECTOR – each event is just a timestamp (epoch ms) */
    private inner class StepDetectorSensorWrapper :
        BaseSensorWrapper<Long>(
            Sensor.TYPE_STEP_DETECTOR,
            samplingUs = SensorManager.SENSOR_DELAY_NORMAL
        ) {
        override fun onSensorChanged(event: SensorEvent) {
            emit(System.currentTimeMillis())
        }
    }

    /** Generic float[] sensor with low sample-rate (e.g. pressure 1 Hz) */
    private inner class FloatArraySensorWrapper(
        sensorType: Int,
        samplingHz: Int
    ) : BaseSensorWrapper<FloatArray>(
        sensorType,
        samplingUs = 1_000_000 / samplingHz
    ) {
        override fun onSensorChanged(event: SensorEvent) {
            emit(event.values.clone())
        }
    }

    /** Off-body Boolean wrapper – 1 == on-body, 0 == off-body */
    private inner class BooleanSensorWrapper(sensorType: Int) :
        BaseSensorWrapper<Boolean>(sensorType, SensorManager.SENSOR_DELAY_NORMAL) {
        override fun onSensorChanged(event: SensorEvent) {
            val onBody = Math.round(event.values[0]) == 1
            emit(onBody)
        }
    }

    /** Heart-rate sensor wrapper (framework TYPE_HEART_RATE). Wear OS specific
     *  Health-Services implementation will replace / extend this later. */
    private inner class HeartRateSensorWrapper :
        BaseSensorWrapper<Float>(
            Sensor.TYPE_HEART_RATE,
            samplingUs = 1_000_000 // 1 Hz
        ) {
        override fun onSensorChanged(event: SensorEvent) {
            emit(event.values.firstOrNull() ?: return)
        }
    }
}