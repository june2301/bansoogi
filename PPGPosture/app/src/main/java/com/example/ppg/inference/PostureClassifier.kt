package com.example.ppg.inference

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.json.JSONObject
import java.io.File
import kotlin.math.exp

class PostureClassifier(
    private val ctx: Context,
    private val modelName: String
) {
    private val tflite: Interpreter = ModelManager.get(ctx, modelName)
    private val input = Array(1) { FloatArray(FEATURE_DIM) }
    private val output = Array(1) { FloatArray(OUTPUT_CLASSES) }

    // per-subject means & scale factors
    private val subjectMeans = FloatArray(FEATURE_DIM)
    private val scaleFactor = FloatArray(FEATURE_DIM) { 1f }

    init {
        loadCalibration()         // load per-subject means from calib.json
        computeScaleFactors()     // build multiplicative offsets
    }

    /**
     * Load calibration (subject) means from calib.json → "calib_means"
     */
    private fun loadCalibration() {
        try {
            val file = File(ctx.filesDir, "calib.json")
            if (!file.exists()) return
            val root = JSONObject(file.readText())
            val calibObj = root.optJSONObject("calib_means") ?: return
            FEAT_NAMES.forEachIndexed { i, key ->
                subjectMeans[i] = calibObj.optDouble(key, 0.0).toFloat()
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    /**
     * Compute scale = global_mean / subject_mean for features we know
     */
    private fun computeScaleFactors() {
        for (i in 0 until FEATURE_DIM) {
            val gm = GLOBAL_MEANS[i]
            val sm = subjectMeans[i]
            scaleFactor[i] = if (gm > 0f && sm > 0f) gm / sm else 1f
        }
    }

    /**
     * Classify by applying multiplicative calibration then running TFLite
     */
    fun classify(feats: FloatArray): Int {
        require(feats.size == FEATURE_DIM)
        Log.d(TAG, "raw feats: ${feats.joinToString(", ")}")
        val corrected = FloatArray(FEATURE_DIM) { feats[it] * scaleFactor[it] }
        Log.d(TAG, "corrected feats: ${corrected.joinToString(", ")}")
        for (i in 0 until FEATURE_DIM) input[0][i] = corrected[i]
        tflite.run(input, output)
        return output[0].argMax()
    }

    fun probs(): FloatArray = softmax(output[0])

    private fun FloatArray.argMax(): Int = indices.maxByOrNull { this[it] } ?: 0
    private fun softmax(x: FloatArray): FloatArray {
        val max = x.maxOrNull() ?: 0f
        val exps = x.map { exp((it - max).toDouble()).toFloat() }
        val sum = exps.sum().takeIf { it > 0f } ?: 1f
        return exps.map { it / sum }.toFloatArray()
    }

    companion object {
        private const val FEATURE_DIM = 10
        private const val OUTPUT_CLASSES = 3
        // global dataset means for select features; others=0 → no scaling
        private val GLOBAL_MEANS = floatArrayOf(
            /* pnn50 */   0.5958591f,
            /* rr_mean */ 0f,
            /* hr_mean */ 103.51322f,
            /* rmssd */   0f,
            /* n_peaks */ 0f,
            /* crest_t */ 0f,
            /* dwell_t */ 0f,
            /* pwtf */    0.56113485f,
            /* kurtosis*/ 0.8517715f,
            /* skew */    0f
        )
        private val FEAT_NAMES = listOf(
            "pnn50","rr_mean","hr_mean","rmssd","n_peaks",
            "crest_t","dwell_t","pwtf","kurtosis","skew"
        )
    }
}