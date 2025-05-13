package com.ddc.bansoogi.activity

import kotlin.math.abs

/**
 * Utility object for calculating Simple Moving Average (SMA) on a 5-second
 * window of linear-acceleration samples (x,y,z interleaved). Can be reused in
 * unit tests or other classifiers.
 */
object SmaFallbackClassifier {

    /**
     * @param window float array where the first three values are (x,y,z) of the
     *               oldest sample and so on – thus size must be multiple of 3.
     * @return SMA value in g (
     */
    fun computeSma(window: FloatArray): Float {
        val n = window.size / 3
        if (n == 0) return 0f
        var sum = 0f
        var i = 0
        while (i < window.size) {
            sum += abs(window[i]) + abs(window[i + 1]) + abs(window[i + 2])
            i += 3
        }
        return sum / n
    }
}