// build.gradle (app 모듈) 에
testImplementation "junit:junit:4.13.2"
testImplementation "org.json:json:20230227"

// src/test/java/com/example/ppg/FeatureSyncTest.kt
package com.example.ppg

import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals

class FeatureSyncTest {
    companion object {
        const val FS = 25
    }

    // Python 과 똑같이 band-pass, detrend 함수를 복붙
    fun detrend(x: FloatArray): FloatArray { /* ... */ }
    fun zeroPhaseFilter(x: FloatArray): FloatArray { /* ... */ }
    fun extractFeatures10s(x: FloatArray): Pair<Int, Float> {
        // peaks/troughs 최소간격 적용 코드
        val rawPeaks = mutableListOf<Int>()
        for (i in 1 until x.lastIndex) if (x[i] > x[i-1] && x[i] > x[i+1]) rawPeaks += i
        val minDist = (FS * 0.4).toInt()
        val peaks = mutableListOf<Int>()
        for (p in rawPeaks) if (peaks.isEmpty() || p-peaks.last() >= minDist) peaks += p

        val rr = peaks.zipWithNext{ a,b -> (b-a).toFloat()/FS }
        val rrMean = if (rr.isNotEmpty()) rr.average().toFloat() else Float.NaN
        return peaks.size to rrMean
    }

    @Test fun testPythonKotlinMatch() {
        // 1) JSON 로드
        val text = java.io.File("recordings/subject01/1747...json").readText()
        val root = JSONObject(text)
        val arr = root.getJSONObject("data")
                      .getJSONObject("ppg_continuous")
                      .getJSONArray("green")
        // 2) FloatArray 로 변환 + 디노말라이즈
        val raw = FloatArray(arr.length()) { i -> arr.getInt(i)/4096f }
        // 3) 워밍업 & 전처리 & 윈도우
        val trimmed = raw.dropWhileIndexed { i,_ -> i < FS*5 }.toFloatArray()
        val x = zeroPhaseFilter(detrend(trimmed))
        val window = x.sliceArray(0 until FS*10)
        // 4) Kotlin 값 추출
        val (nPeaksK, rrMeanK) = extractFeatures10s(window)
        println("KT peaks=$nPeaksK rr_mean=$rrMeanK")
        // 5) Python 값 (하드코딩하거나 앞서 찍은 값으로) 과 비교
        val expectedPeaks = /* Python 에서 얻은 값 */
        val expectedRrMean = /* Python 에서 얻은 값 */
        assertEquals(expectedPeaks, nPeaksK, "n_peaks mismatch")
        assertEquals(expectedRrMean, rrMeanK, 0.005f, "rr_mean mismatch")
    }
}
