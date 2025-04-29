package com.example.prototype

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * End-to-end activity pipeline as described in the v2 spec.
 *
 * Usage
 * -----
 *   val state = ActivityPipeline.feed(t, ax, ay, az, gx, gy, gz, pressure, stepEvt)
 */
object ActivityPipeline {

    // ------------ Tick parameters (raw → flags) -------------
    private const val GYRO_T = 3.0            // rad/s RMS (≈ 170 deg/s)
    private const val ACCEL_T = 1.5           // m/s² deviation from 1 g (~9.81)

    // ------------ Window parameters -------------------------
    private const val WINDOW_SEC = 2.0        // seconds
    private const val WINDOW_HOP_SEC = 1.0    // 50 % overlap

    // ------------ Static posture thresholds -----------------
    private const val PITCH_SIT_MIN = 20.0 // deg
    private const val PITCH_SIT_MAX = 70.0 // deg
    private const val ROLL_SIT_MAX = 30.0 // deg

    private const val PITCH_STAND_MAX = 10.0 // deg

    private const val PITCH_LYING_MIN = 75.0 // deg (absolute)

    // Gyro quiet threshold for static postures (reuse GYRO_T)

    // ------------ Hysteresis -------------------------------
    private const val MIN_HOLD_MS = 1_500L    // 1.5 s

    // --------------------------------------------------------

    private data class Tick(
        val ts: Long,
        val isDynamic: Boolean,
        val isStatic: Boolean,
        val pitch: Double,
        val roll: Double,
        val gyroRms: Double,
        val stepEvt: Boolean,
        val alt: Float,
    )

    // Circular buffer for window
    private val window = ArrayDeque<Tick>()

    private var lastEmitState: ActivityState = ActivityState.TRANSIENT
    private var lastEmitTime: Long = 0L

    private val stairDetector = StairUpDetector()

    /**
     * Feed one new sensor sample (tick) and get updated ActivityState.
     * @return most recent state after state-machine.
     */
    fun feed(
        timestamp: Long,
        ax: Double,
        ay: Double,
        az: Double,
        gx: Double,
        gy: Double,
        gz: Double,
        pressure: Double, // hPa – used externally for altitude LPF. Here we just keep raw.
        stepEvt: Boolean,
    ): ActivityState {
        // ----------------------------------------------------
        // 1. Tick classifier (isDynamic / isStatic)
        // ----------------------------------------------------
        val gyroRms = sqrt(gx * gx + gy * gy + gz * gz)
        val accMag = sqrt(ax * ax + ay * ay + az * az)
        val isDynamic = gyroRms > GYRO_T || abs(accMag - 9.81) > ACCEL_T

        // Static if not dynamic & orientation within any static band
        val (pitch, roll) = computePitchRoll(ax, ay, az)
        val absPitch = abs(pitch)
        val absRoll = abs(roll)
        val inStaticBand =
            absPitch <= PITCH_STAND_MAX ||
                // standing band (near vertical)
                (absPitch >= PITCH_SIT_MIN && absPitch <= PITCH_SIT_MAX && absRoll <= ROLL_SIT_MAX) ||
                absPitch >= PITCH_LYING_MIN
        val isStatic = !isDynamic && inStaticBand

        // Altitude – rough, convert pressure diff to metres using Δh ≈ ΔP × 8.3
        val altitude = (pressure * -8.3).toFloat() // convert to metres scale as Float

        // Add to window
        val tick = Tick(timestamp, isDynamic, isStatic, pitch, roll, gyroRms, stepEvt, altitude)
        window += tick
        dropOldTicks(timestamp)

        // ----------------------------------------------------
        // 2. Window aggregator
        // ----------------------------------------------------
        val N = window.size
        if (N == 0) return ActivityState.TRANSIENT
        val dynamicRatio = window.count { it.isDynamic }.toDouble() / N
        val staticRatio = window.count { it.isStatic }.toDouble() / N

        val candidateTop = when {
            dynamicRatio > 0.6 -> "DYNAMIC"
            staticRatio > 0.6 -> "STATIC"
            else -> "TRANSIENT"
        }

        // ----------------------------------------------------
        // 3. State-2 detailed classification
        // ----------------------------------------------------
        val detailedState: ActivityState = when (candidateTop) {
            "DYNAMIC" -> classifyDynamic(window.last())
            "STATIC" -> classifyStatic(window.last())
            else -> ActivityState.TRANSIENT
        }

        // ----------------------------------------------------
        // 4. Hysteresis / hold
        // ----------------------------------------------------
        val now = timestamp
        if (detailedState != lastEmitState && (now - lastEmitTime) < MIN_HOLD_MS) {
            // keep previous until min hold passes
            return lastEmitState
        }

        // emit new state
        lastEmitState = detailedState
        lastEmitTime = now
        return detailedState
    }

    // --------------------------------------------------------
    // Helper functions
    // --------------------------------------------------------

    private fun dropOldTicks(now: Long) {
        val windowMs = (WINDOW_SEC * 1_000).toLong()
        // Keep half-window overlap: remove ticks older than windowMs
        while (window.isNotEmpty() && now - window.first().ts > windowMs) {
            window.removeFirst()
        }
    }

    private fun classifyDynamic(t: Tick): ActivityState {
        // Priority 1: stair up detector
        val stair = stairDetector.onSensorTick(t.ts, t.alt, t.stepEvt)
        if (stair) return ActivityState.STAIR_UP

        // TODO: integrate ActivityRecognitionManager for RUNNING/WALKING/EXERCISE.
        // For now, return DYNAMIC_GENERIC.
        return ActivityState.DYNAMIC_GENERIC
    }

    private fun classifyStatic(t: Tick): ActivityState {
        return when {
            abs(t.pitch) >= PITCH_LYING_MIN -> ActivityState.LYING
            abs(t.pitch) <= PITCH_STAND_MAX -> ActivityState.STANDING
            (abs(t.pitch) >= PITCH_SIT_MIN && abs(t.pitch) <= PITCH_SIT_MAX && abs(t.roll) <= ROLL_SIT_MAX) -> ActivityState.SITTING
            else -> ActivityState.TRANSIENT
        }
    }

    private fun computePitchRoll(
        ax: Double,
        ay: Double,
        az: Double,
    ): Pair<Double, Double> {
        val pitch = Math.toDegrees(atan2(-ax, sqrt(ay * ay + az * az)))
        val roll = Math.toDegrees(atan2(ay, az))
        return pitch to roll
    }
}
