package com.ddc.bansoogi.activity

import kotlin.math.abs

/** 선형 가속도의 5 s SMA 계산 유틸 */
object SmaFallbackClassifier {
    /**
     * @param window float array of linear‐only magnitudes
     * @return SMA value in m/s²
     */
    fun computeSma(window: FloatArray): Float {
        if (window.isEmpty()) return 0f
        var sum = 0f
        for (v in window) {
            sum += abs(v)
        }
        return sum / window.size
    }
}
