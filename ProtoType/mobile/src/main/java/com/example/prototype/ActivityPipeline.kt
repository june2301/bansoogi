// ActivityPipeline.kt
package com.example.prototype

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/* ---------------------------------------------------------
 *  (1) Import
 * --------------------------------------------------------- */
import com.example.prototype.ActivityState
import com.example.prototype.StairUpDetector

/* ---------------------------------------------------------
 *  (3) ActivityPipeline
 * --------------------------------------------------------- */
object ActivityPipeline {
    // ---------- Tick 파라미터 ----------
    private const val GYRO_T = 3.0 // rad/s RMS
    private const val ACCEL_T = 1.5 // m/s² dev from 1 g

    // ---------- Window 파라미터 ----------
    private const val WINDOW_SEC = 2.0
    private const val WINDOW_HOP_SEC = 1.0

    // ---------- Static posture(앉기/눕기) ----------
    private const val PITCH_SIT_MIN = 20.0
    private const val PITCH_SIT_MAX = 70.0
    private const val ROLL_SIT_MAX = 90.0
    private const val PITCH_LYING_MIN = 75.0
    private const val PITCH_STAND_MAX = 10.0 // standing 유지 시

    // ---------- Hysteresis ----------
    private const val MIN_HOLD_MS = 1_500L // 1.5 s

    // ---------- Dynamic 분류 추가 파라미터 ----------
    private const val RUN_GYRO_T = 6.0
    private const val CADENCE_RUN_MIN = 140.0
    private const val CADENCE_WALK_MIN = 60.0
    private const val CADENCE_WINDOW_MS = 5_000L

    // ---------- 내부 자료구조 ----------
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

    private val window = ArrayDeque<Tick>()
    private val stepEvents = ArrayDeque<Long>()
    private val stairDetector = StairUpDetector()

    @Volatile
    private var externalDynamicState: ActivityState? = null

    private var lastEmitState: ActivityState = ActivityState.TRANSIENT
    private var lastEmitTime: Long = 0L

    // ---------- 외부 API 업데이트 ----------
    fun updateExternalDynamic(state: ActivityState) {
        externalDynamicState = state
    }

    // ---------- 메인 엔트리 ----------
    fun feed(
        timestamp: Long,
        ax: Double,
        ay: Double,
        az: Double,
        gx: Double,
        gy: Double,
        gz: Double,
        pressure: Double, // hPa
        stepEvt: Boolean,
    ): ActivityState {
        // ---- 1. Tick classifier ----
        val gyroRms = sqrt(gx * gx + gy * gy + gz * gz)
        val accMag = sqrt(ax * ax + ay * ay + az * az)
        val isDynamic = gyroRms > GYRO_T || abs(accMag - 9.81) > ACCEL_T

        val (pitch, roll) = computePitchRoll(ax, ay, az)
        val absPitch = abs(pitch)
        val absRoll = abs(roll)
        val inStaticBand =
            absPitch <= PITCH_STAND_MAX ||
                // standing
                (absPitch in PITCH_SIT_MIN..PITCH_SIT_MAX && absRoll <= ROLL_SIT_MAX) ||
                absPitch >= PITCH_LYING_MIN
        val isStatic = !isDynamic && inStaticBand

        // Δh ≈ −8.3 × ΔP  (간이 변환)
        val altitude = (pressure * -8.3).toFloat()

        val tick = Tick(timestamp, isDynamic, isStatic, pitch, roll, gyroRms, stepEvt, altitude)
        window += tick
        if (stepEvt) stepEvents += timestamp
        pruneOldSteps(timestamp)
        dropOldTicks(timestamp)

        // ---- 2. Window aggregator ----
        val N = window.size
        val dynamicRatio = window.count { it.isDynamic }.toDouble() / N
        val staticRatio = window.count { it.isStatic }.toDouble() / N
        val candidateTop =
            when {
                dynamicRatio > 0.3 -> "DYNAMIC"
                staticRatio > 0.3 -> "STATIC"
                else -> "TRANSIENT"
            }

        // ---- 3. Detailed classification ----
        val detailedState =
            when (candidateTop) {
                "DYNAMIC" -> classifyDynamic(window.last())
                "STATIC" -> classifyStatic(window.last())
                else -> ActivityState.TRANSIENT
            }

        // ---- 4. Hysteresis ----
        val now = timestamp
        if (detailedState != lastEmitState && now - lastEmitTime < MIN_HOLD_MS) {
            return lastEmitState // hold 이전 상태
        }
        lastEmitState = detailedState
        lastEmitTime = now
        return detailedState
    }

    // ---------- 내부 헬퍼 ----------

    private fun dropOldTicks(now: Long) {
        val windowMs = (WINDOW_SEC * 1_000).toLong()
        while (window.isNotEmpty() && now - window.first().ts > windowMs) {
            window.removeFirst()
        }
    }

    private fun pruneOldSteps(now: Long) {
        while (stepEvents.isNotEmpty() && now - stepEvents.first() > CADENCE_WINDOW_MS) {
            stepEvents.removeFirst()
        }
    }

    private fun computeCadence(now: Long): Double {
        pruneOldSteps(now)
        if (stepEvents.size < 2) return 0.0
        val duration = (stepEvents.last() - stepEvents.first()).coerceAtLeast(1)
        return (stepEvents.size - 1) * 60_000.0 / duration
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

    private fun classifyDynamic(tick: Tick): ActivityState {
        externalDynamicState?.let {
            externalDynamicState = null // consume once
            return it
        }

        if (stairDetector.onSensorTick(tick.ts, tick.alt, tick.stepEvt)) {
            return ActivityState.STAIR_UP
        }

        val cadence = computeCadence(tick.ts)
        return when {
            cadence >= CADENCE_RUN_MIN || tick.gyroRms >= RUN_GYRO_T -> ActivityState.RUNNING
            cadence >= CADENCE_WALK_MIN -> ActivityState.WALKING
            else -> ActivityState.EXERCISE
        }
    }

    private fun classifyStatic(tick: Tick): ActivityState =
        when {
            abs(tick.pitch) >= PITCH_LYING_MIN -> ActivityState.LYING
            abs(tick.pitch) in PITCH_SIT_MIN..PITCH_SIT_MAX &&
                abs(tick.roll) <= ROLL_SIT_MAX -> ActivityState.SITTING
            else -> ActivityState.TRANSIENT
        }
}
