/* ────────────────────────────────────────────────────────────────
 * AndroidSensorManager.kt
 *   – 모든 센서 + UserActivityState(Health Services) 을 Flow 로 제공
 * ──────────────────────────────────────────────────────────────── */
package com.ddc.bansoogi.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

class AndroidSensorManager(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val TAG = "AndroidSensorManager"

    // ────────────────────────────────────────────────────────────
    //  Generic base wrapper
    // ────────────────────────────────────────────────────────────
    private abstract inner class BaseSensorWrapper<T : Any>(
        private val sensorType: Int,
        private val samplingUs: Int
    ) : SensorEventListener {
        private val sensor: Sensor? = sensorManager.getDefaultSensor(sensorType)
        private val _events = MutableSharedFlow<T>(extraBufferCapacity = 64)
        val events: SharedFlow<T> = _events.asSharedFlow()

        fun start() {
            sensor?.let { sensorManager.registerListener(this, it, samplingUs) }
        }
        fun stop() = sensorManager.unregisterListener(this)
        protected fun emit(v: T) = _events.tryEmit(v)
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    // 실제 ACCELEROMETER 사용
    private inner class AccelerometerSensorWrapper :
        BaseSensorWrapper<FloatArray>(Sensor.TYPE_ACCELEROMETER, 1_000_000 / 50) {

        override fun onSensorChanged(e: SensorEvent) {
            val values = e.values.clone()
//            Log.d(ACCEL_TAG, "Accel → x=${values[0]}, y=${values[1]}, z=${values[2]}")
            emit(values)
        }
    }

    private inner class StepDetectorSensorWrapper :
        BaseSensorWrapper<Long>(Sensor.TYPE_STEP_DETECTOR, SensorManager.SENSOR_DELAY_NORMAL) {
        override fun onSensorChanged(event: SensorEvent) {
            val timestamp = System.currentTimeMillis()
            emit(timestamp)
            Log.d(TAG, "StepDetector event at $timestamp")
        }

    }

    private inner class FloatArraySensorWrapper(type: Int, hz: Int) :
        BaseSensorWrapper<FloatArray>(type, 1_000_000 / hz) {
        override fun onSensorChanged(e: SensorEvent) {
            emit(e.values.clone())
        }
    }

    private inner class BooleanSensorWrapper(type: Int) :
        BaseSensorWrapper<Boolean>(type, SensorManager.SENSOR_DELAY_NORMAL) {
        override fun onSensorChanged(e: SensorEvent) {
            emit(e.values[0].roundToInt() == 1)
        }
    }

    private inner class HeartRateSensorWrapper :
        BaseSensorWrapper<Float>(Sensor.TYPE_HEART_RATE, 1_000_000) {
        override fun onSensorChanged(e: SensorEvent) {
            e.values.firstOrNull()?.let { emit(it) }
        }
    }

    // ────────────────────────────────────────────────────────────
    //  public streams & control
    // ────────────────────────────────────────────────────────────
    private val offBodyWrapper      by lazy { BooleanSensorWrapper(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT) }
    private val accelWrapper        by lazy { AccelerometerSensorWrapper() }
    private val stepDetectorWrapper by lazy { StepDetectorSensorWrapper() }
    private val pressureWrapper     by lazy { FloatArraySensorWrapper(Sensor.TYPE_PRESSURE, 1) }
    private val heartRateWrapper    by lazy { HeartRateSensorWrapper() }

    val isOffBody: SharedFlow<Boolean>          = offBodyWrapper.events
    val linearAcceleration: SharedFlow<FloatArray> = accelWrapper.events
    val stepDetector: SharedFlow<Long>          = stepDetectorWrapper.events
    val pressure: SharedFlow<FloatArray>        = pressureWrapper.events
    val heartRate: SharedFlow<Float>            = heartRateWrapper.events

    fun startAll() {
        offBodyWrapper.start()
        accelWrapper.start()
        stepDetectorWrapper.start()
        pressureWrapper.start()
        heartRateWrapper.start()
    }

    fun stopAll() {
        offBodyWrapper.stop()
        accelWrapper.stop()
        stepDetectorWrapper.stop()
        pressureWrapper.stop()
        heartRateWrapper.stop()
    }
}