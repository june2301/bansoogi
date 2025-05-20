// app/src/main/java/com/example/ppg/inference/PostureClassifier.kt
package com.example.ppg.inference

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.File
import kotlin.math.exp

/**
 * Posture classifier with *user‑specific* Z‑score followed by
 * re‑alignment to the **training** feature distribution.
 *
 * flow:
 *   1) quantile‑clip (user clip_bounds)
 *   2) z_user = (x – μ_user) / σ_user
 *   3) x_final = z_user * σ_train + μ_train
 *   4) TFLite inference
 */
class PostureClassifier(
    private val ctx: Context,
    private val modelName: String
) {
    private val tflite: Interpreter = ModelManager.get(ctx, modelName)
    private val input  = Array(1) { FloatArray(FEATURE_DIM) }
    private val output = Array(1) { FloatArray(OUTPUT_CLASSES) }

    /* ---------- calibration data ---------- */
    private val clipBounds  = Array(FEATURE_DIM) { Float.NEGATIVE_INFINITY to Float.POSITIVE_INFINITY }

    // user stats
    private val muUser    = FloatArray(FEATURE_DIM) { 0f }
    private val sigmaUser = FloatArray(FEATURE_DIM) { 1f }

    // training stats (read‑only, from assets/calib.json)
    private val muTrain    = FloatArray(FEATURE_DIM) { 0f }
    private val sigmaTrain = FloatArray(FEATURE_DIM) { 1f }

    init {
        loadUserCalibration()
        loadTrainStats()
        Log.i(TAG, "⚙️ PostureClassifier initialised (user ↦ train mapping)")
    }

    /* ---------------------------------------------------------------------- */
    private fun loadUserCalibration() {
        try {
            val fp = File(ctx.filesDir, "calib.json")
            if (!fp.exists()) return
            val root = JSONObject(fp.readText())

            /* 1) clip bounds */
            root.optJSONObject("clip_bounds")?.let { cb ->
                FEAT_NAMES.forEachIndexed { i, key ->
                    cb.optJSONArray(key)?.let { arr ->
                        clipBounds[i] = arr.optDouble(0).toFloat() to arr.optDouble(1).toFloat()
                    }
                }
            }
            /* 2) user μ/σ */
            root.optJSONObject("stats")?.let { st ->
                FEAT_NAMES.forEachIndexed { i, k ->
                    st.optJSONObject(k)?.let { f ->
                        muUser[i]    = f.optDouble("mu", 0.0).toFloat()
                        sigmaUser[i] = f.optDouble("sigma", 1.0).toFloat()
                    }
                }
            }
        } catch (e: Exception) { Log.w(TAG, "loadUserCalibration failed", e) }
    }

    private fun loadTrainStats() {
        try {
            val txt = ctx.assets.open("calib.json").bufferedReader().use { it.readText() }
            val root = JSONObject(txt)
            root.optJSONObject("stats")?.let { st ->
                FEAT_NAMES.forEachIndexed { i, k ->
                    st.optJSONObject(k)?.let { f ->
                        muTrain[i]    = f.optDouble("mu", 0.0).toFloat()
                        sigmaTrain[i] = f.optDouble("sigma", 1.0).toFloat()
                    }
                }
            }
        } catch (e: Exception) { Log.w(TAG, "loadTrainStats failed", e) }
    }

    /* ---------------------------------------------------------------------- */
    fun classify(raw: FloatArray): Int {
        require(raw.size == FEATURE_DIM)
        Log.d(TAG, "1️⃣ raw ▶ ${raw.joinToString()}")

        /* 1) quantile clip (user bounds) */
        val clipped = FloatArray(FEATURE_DIM) { i ->
            raw[i].coerceIn(clipBounds[i].first, clipBounds[i].second)
        }
        Log.d(TAG, "2️⃣ clipped ▶ ${clipped.joinToString()}")

        /* 2) user Z‑score */
        val zUser = FloatArray(FEATURE_DIM) { i ->
            val sd = sigmaUser[i]; if (sd > 0f) (clipped[i] - muUser[i]) / sd else 0f
        }
        Log.d(TAG, "3️⃣ z_user ▶ ${zUser.joinToString()}")

        /* 3) realign to *training* distribution */
//        val aligned = FloatArray(FEATURE_DIM) { i ->
//            zUser[i] * sigmaTrain[i] + muTrain[i]
//        }
//        Log.d(TAG, "4️⃣ aligned ▶ ${aligned.joinToString()}")
        val aligned = zUser
        Log.d(TAG, "4️⃣ aligned (no train mapping) ▶ ${aligned.joinToString()}")

        /* 4) TFLite inference */
        for (i in 0 until FEATURE_DIM) input[0][i] = aligned[i]
        tflite.run(input, output)
        // 🟡 누움 라벨(1번)에 0.5 가중치 부여
        val pred = output[0].argMax()
        Log.d(TAG, "5️⃣ output ▶ ${output[0].joinToString()} argmax=$pred")
        return pred
    }

    fun probs(): FloatArray = softmax(output[0])

    /* ---------------- helpers ---------------- */
    private fun FloatArray.argMax(): Int = indices.maxByOrNull { this[it] } ?: 0
    private fun softmax(x: FloatArray): FloatArray {
        val m = x.maxOrNull() ?: 0f
        val exps = x.map { exp((it - m).toDouble()).toFloat() }
        val s = exps.sum().takeIf { it > 0f } ?: 1f
        return exps.map { it / s }.toFloatArray()
    }

    companion object {
        private const val FEATURE_DIM = 10
        private const val OUTPUT_CLASSES = 3
        private val FEAT_NAMES = listOf(
            "pnn50","rr_mean","hr_mean","rmssd","n_peaks",
            "crest_t","dwell_t","pwtf","kurtosis","skew"
        )
        private const val TAG = "PostureClassifier"
    }
}
