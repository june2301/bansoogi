package com.example.prototype.pose

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Implements the Dynamic-Skip heuristic described in README.
 * - SMA (sum(|a|) / N) threshold 0.30 g (≈2.943 m/s²)
 * - Gyro RMS threshold 0.05 rad/s
 * If BOTH below their thresholds, we consider the window static ⇒ OK to infer.
 * Otherwise, skip inference because movement is dynamic.
 */
object DynamicSkip {
    private const val G_TO_MS2 = 9.80665f
    private const val SMA_THRESH = 0.30f * G_TO_MS2 // ≈2.94 m/s²
    private const val GYRO_RMS_THRESH = 0.05f // rad/s

    /**
     * @param accWindow flattened ax,ay,az… length = 3*N
     * @return true if window should be SKIPPED (i.e., movement too strong)
     */
    fun shouldSkip(accWindow: FloatArray): Boolean {
        val n = accWindow.size / 3
        var smaSum = 0f
        for (i in 0 until n) {
            val ax = accWindow[3 * i]
            val ay = accWindow[3 * i + 1]
            val az = accWindow[3 * i + 2]
            smaSum += abs(ax) + abs(ay) + abs(az)
        }
        val sma = smaSum / n
        return sma >= SMA_THRESH // gyro not available (gyroscope not collected), so only SMA
    }

    /**
     * @param linWindow flattened lx,ly,lz… length = 3*N
     * @return true if window should be SKIPPED (i.e., movement too strong)
     */
    fun shouldSkipLinear(linWindow: FloatArray): Boolean {
        val n = linWindow.size / 3
        var smaSum = 0f
        for (i in 0 until n) {
            smaSum += abs(linWindow[3 * i]) + abs(linWindow[3 * i + 1]) + abs(linWindow[3 * i + 2])
        }
        val sma = smaSum / n
        return sma >= SMA_THRESH
    }

    // Overload when gyroscope also given
    fun shouldSkip(
        accWindow: FloatArray,
        gyroWindow: FloatArray,
    ): Boolean {
        val accSkip = shouldSkip(accWindow)
        val n = gyroWindow.size / 3
        var rmsSum = 0f
        for (i in 0 until n) {
            val gx = gyroWindow[3 * i]
            val gy = gyroWindow[3 * i + 1]
            val gz = gyroWindow[3 * i + 2]
            rmsSum += gx * gx + gy * gy + gz * gz
        }
        val rms = sqrt(rmsSum / n)
        val gyroSkip = rms >= GYRO_RMS_THRESH
        return accSkip || gyroSkip
    }
}
