package com.example.ppg.sensor

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ppg.filter.Butterworth
import com.samsung.android.service.health.tracking.*
import com.samsung.android.service.health.tracking.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.*

/**
 * Calibration & rule‑based inference service **fully aligned** with the Python
 * training pipeline (HRV‑10, detrend → z‑score → zero‑phase Butterworth 0.5‑5 Hz).
 *
 * ‑ Calibration mode: 35 s 녹화 → 모든 10‑피처 윈도우를 calib.json/raw 에 저장.
 * ‑ Live rule mode: 짧은 10‑피처 벡터에서 간단한 z‑score 룰로 자세 판정 (기존과 동일).
 */
class CalibRuleService : Service() {

    /* ───────────── constants ───────────── */
    companion object {
        const val ACTION_CALIB      = "com.example.ppg.CALIB"
        const val ACTION_LIVE       = "com.example.ppg.LIVE"
        const val ACTION_STOP       = "com.example.ppg.STOP"
        const val ACTION_RULE       = "com.example.ppg.RULE"
        const val ACTION_CALIB_DONE = "com.example.ppg.CALIB_DONE"
        const val FEATURE_DIM = 10
        val FEAT_NAMES = arrayOf("mean", "std", "rms", "max", "min", "zc", "ssc", "wl", "kurt", "skew")
        private const val TAG = "CalibRuleSvc"
    }
    private val FS = 25
    private val WIN_SEC = 10
    private val STEP_SEC   = 1
    private val WINDOW_SIZE = FS * WIN_SEC      // 250
    private val STEP_SIZE   = FS * STEP_SEC        // 25
    private val WIN_SAMPLES = FS * WIN_SEC
    private val MIN_DIST = (FS * 0.4).toInt()
    private val NOTI_CH = "CALIB_RULE_CH"
    private val NOTI_ID = 2

    /* ───────────── runtime state ───────────── */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var hts: HealthTrackingService
    private lateinit var ppgTracker: HealthTracker
    private var recording = false
    private var liveMode  = false
    private var warmupFrames = 0
    private val rawBuf = ArrayDeque<Float>()
    private var frameCnt = 0

    // calibration collection
    private val calibMutex = Mutex()
    private var currentLabel = "unknown"
    private val calibMap = mutableMapOf<String, MutableList<FloatArray>>()

    // raw green μ/σ for z‑score (from assets/calib.json)
    private var gMu = 0f
    private var gSigma = 1f

    /* ───────────────────────────────────── */
    override fun onCreate() { super.onCreate(); createChannel(); loadRawStats() }
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, id: Int): Int {
        when (i?.action) {
            ACTION_CALIB -> startCalibration(i.getStringExtra("label") ?: "unknown")
            ACTION_LIVE  -> startLive()
            ACTION_STOP  -> stopSelf()
        }
        return START_STICKY
    }

    /* ───────────── calibration / live ───────────── */
    private fun startCalibration(label: String) {
        liveMode = false; currentLabel = label; beginSensors()
        startForeground(NOTI_ID, notif("Calibrating $label…"))
        scope.launch {
            delay(65_000)
            stopSensors(); writeCalibJson()
            sendBroadcast(Intent(ACTION_CALIB_DONE).setPackage(packageName).putExtra("label", label))
            stopSelf()
        }
    }
    private fun startLive() {
        liveMode = true; beginSensors()
        startForeground(NOTI_ID, notif("Rule‑based Inference"))
    }

    private fun beginSensors() {
        if (recording) return
        recording = true; warmupFrames = 0; rawBuf.clear()
        hts = HealthTrackingService(conn, this)
        hts.connectService()
    }
    private fun stopSensors() {
        recording = false
        runCatching { ppgTracker.unsetEventListener() }
        runCatching { hts.disconnectService() }
    }

    /* ───────────── Samsung SDK callbacks ───────────── */
    private val conn = object : ConnectionListener {
        override fun onConnectionSuccess() {
            ppgTracker = hts.getHealthTracker(HealthTrackerType.PPG_CONTINUOUS, setOf(PpgType.GREEN))
            ppgTracker.setEventListener(ppgListener)
            runCatching { ppgTracker.javaClass.getMethod("start").invoke(ppgTracker) }
        }
        override fun onConnectionFailed(e: HealthTrackerException) { Log.e(TAG,"connect fail ${e.errorCode}"); stopSelf() }
        override fun onConnectionEnded() {}
    }

    private val ppgListener = object : HealthTracker.TrackerEventListener {
        override fun onDataReceived(points: List<DataPoint>) {
            if (!recording) return
            points.forEach { dp ->
                val gRaw = (dp.getValue(ValueKey.PpgSet.PPG_GREEN) as? Number)?.toLong() ?: 0L
                val g = gRaw.toFloat() / 4096f

                if (warmupFrames < FS * 5) { warmupFrames++; return@forEach } // 5 s 워밍업 동일

                rawBuf.addLast(g)
                if (rawBuf.size > WIN_SAMPLES) rawBuf.removeFirst()
                if (rawBuf.size == WIN_SAMPLES) {
                    val feats = extractFeatures10s(rawBuf.toFloatArray())
                    if (liveMode) sendRuleBroadcast(calcRule(feats))
                    else scope.launch { calibMutex.withLock {
                        calibMap.getOrPut(currentLabel) { mutableListOf() }.add(feats)
                    } }
                }
            }
        }
        override fun onFlushCompleted() {}
        override fun onError(e: HealthTracker.TrackerError) { Log.e(TAG, "PPG error $e") }
    }

    /* ───────────── feature extraction (HRV‑10 full) ───────────── */
    private fun detrend(x: FloatArray): FloatArray {
        val n = x.size; val xsBar = (n - 1) / 2f
        var sX = 0f; var sY = 0f; var sXY = 0f; var sXX = 0f
        for (i in x.indices) {
            val dx = i - xsBar; sX += dx; sY += x[i]; sXY += dx * x[i]; sXX += dx * dx
        }
        val slope = if (sXX != 0f) sXY / sXX else 0f
        val intercept = (sY - slope * sX) / n
        return FloatArray(n) { i -> x[i] - (slope * i + intercept) }
    }

    private fun extractFeatures10s(raw: FloatArray): FloatArray {
        val dt = detrend(raw)
        val norm = FloatArray(dt.size) { i -> if (gSigma > 0f) (dt[i] - gMu) / gSigma else dt[i] - gMu }
        val x = Butterworth.filtfilt(norm)

        /* peak & trough */
        val peaks = mutableListOf<Int>()
        val troughs = mutableListOf<Int>()
        for (i in 1 until x.lastIndex) {
            if (x[i] > x[i - 1] && x[i] > x[i + 1]) peaks += i
            if (x[i] < x[i - 1] && x[i] < x[i + 1]) troughs += i
        }
        fun filterByMinDist(list: List<Int>): List<Int> {
            val out = mutableListOf<Int>()
            var last = -MIN_DIST
            list.forEach { p -> if (p - last >= MIN_DIST) { out += p; last = p } }
            return out
        }
        val pk = filterByMinDist(peaks)
        val tr = filterByMinDist(troughs)

        /* HRV‑10 */
        val rr = pk.zipWithNext { a, b -> (b - a).toFloat() / FS }
        val feat = FloatArray(10)
        if (rr.isNotEmpty()) {
            feat[4] = pk.size.toFloat() // n_peaks
            val rrM = rr.average().toFloat(); feat[1] = rrM; feat[2] = if (rrM > 0f) 60f / rrM else 0f
        }
        if (rr.size > 1) {
            val dif = rr.zipWithNext { a, b -> b - a }
            feat[3] = sqrt(dif.map { it * it }.average()).toFloat() // rmssd
            feat[0] = dif.count { abs(it) > 0.05f }.toFloat() / dif.size // pnn50
        }
        // crest / dwell / pwtf
        val cycles = pk.mapNotNull { p ->
            val f = tr.lastOrNull { it < p } ?: return@mapNotNull null
            val n = tr.firstOrNull { it > p } ?: return@mapNotNull null
            Triple(f, p, n)
        }
        if (cycles.isNotEmpty()) {
            val ct = cycles.map { (f, p, _) -> (p - f).toFloat() / FS }.average().toFloat()
            val dw = cycles.map { (f, _, n) -> (n - f).toFloat() / FS }.average().toFloat()
            feat[5] = ct; feat[6] = dw; feat[7] = if (dw != 0f) ct / dw else 0f
        }
        // kurtosis & skew
        val mu = x.average().toFloat()
        val m2 = x.map { (it - mu).pow(2) }.average().toFloat()
        val m3 = x.map { (it - mu).pow(3) }.average().toFloat()
        val m4 = x.map { (it - mu).pow(4) }.average().toFloat()
        feat[8] = if (m2 > 1e-6f) (m4 / (m2 * m2) - 3f) else 0f
        feat[9] = if (m2 > 1e-6f) (m3 / m2.pow(1.5f)) else 0f
        return feat
    }

    /* ───────────── rule calc (기존) ───────────── */
    private fun calcRule(f: FloatArray): String {
        val file = File(filesDir, "calib.json"); if (!file.exists()) return "upright‑sitting"
        val root = JSONObject(file.readText())
        val stats = root.optJSONObject("stats") ?: return "upright‑sitting"
        fun z(v: Float, key: String): Float {
            val s = stats.optJSONObject(key) ?: return 0f
            val mu = s.optDouble("mu", 0.0).toFloat(); val sd = s.optDouble("sigma", 1.0).toFloat()
            return if (sd != 0f) (v - mu) / sd else 0f
        }
        val hrZ = z(f[2], "hr_mean"); val pnnZ = z(f[0], "pnn50"); val kurZ = z(f[8], "kurtosis")
        val thr = root.optJSONObject("thresholds") ?: return "upright‑sitting"
        val sup = thr.optJSONObject("supine"); val std = thr.optJSONObject("standing")
        return when {
            sup != null && hrZ < sup.optDouble("hr_z_max", -9.0).toFloat() && pnnZ > sup.optDouble("pnn50_z_min", 0.0).toFloat() -> "supine‑lying"
            std != null && hrZ > std.optDouble("hr_z_min", 9.0).toFloat() && kurZ > std.optDouble("kurtosis_z_min", 0.0).toFloat() -> "standing"
            else -> "upright‑sitting"
        }
    }

    private fun sendRuleBroadcast(label: String) {
        sendBroadcast(Intent(ACTION_RULE).setPackage(packageName).putExtra("rule_label", label))
    }

    /* ───────────── calib.json writer (10‑feat) ───────────── */
    private suspend fun writeCalibJson() = withContext(Dispatchers.IO) {
        if (calibMap.isEmpty()) return@withContext
        val file = File(filesDir, "calib.json")
        val root = runCatching { JSONObject(file.readText()) }.getOrNull() ?: JSONObject()
        val rawObj = root.optJSONObject("raw") ?: JSONObject().also { root.put("raw", it) }

        // ─── 1) raw data append ───────────────────────────
        calibMap.forEach { (label, list) ->
            val arr = rawObj.optJSONArray(label) ?: JSONArray().also { rawObj.put(label, it) }
            list.forEach { window -> arr.put(JSONArray(window.toList())) }
        }
        calibMap.clear()

        // ─── 2) aggregate all windows ──────────────────────
        val allWin = mutableListOf<FloatArray>()
        rawObj.keys().forEach { label ->
            rawObj.optJSONArray(label)?.let { ja ->
                for (i in 0 until ja.length()) {
                    ja.optJSONArray(i)?.let { wa ->
                        allWin += FloatArray(wa.length()) { idx -> wa.optDouble(idx, 0.0).toFloat() }
                    }
                }
            }
        }

        if (allWin.isNotEmpty()) {
            val dim = FEATURE_DIM  // = 10
            val mu = FloatArray(dim)
            val sd = FloatArray(dim)
            val count = allWin.size

            // 2.1) compute μ
            allWin.forEach { w -> w.forEachIndexed { i, v -> mu[i] += v } }
            for (i in 0 until dim) mu[i] /= count

            // 2.2) compute σ
            allWin.forEach { w -> w.forEachIndexed { i, v -> sd[i] += (v - mu[i]).pow(2) } }
            for (i in 0 until dim) sd[i] = sqrt(sd[i] / count)

            // ─── 3) write full stats (μ/σ) for all features ───
            val statsObj = JSONObject()
            FEAT_NAMES.forEachIndexed { i, key ->
                statsObj.put(key, JSONObject().apply {
                    put("mu", mu[i].toDouble())
                    put("sigma", sd[i].toDouble())
                })
            }
            root.put("stats", statsObj)

            // ─── 4) write full clip_bounds (μ±3σ) all features ─
            val clipObj = JSONObject()
            val calibMeansObj = JSONObject()
            FEAT_NAMES.forEachIndexed { i, key ->
                val lo = (mu[i] - 3f * sd[i]).coerceAtLeast(0f)
                val hi = (mu[i] + 3f * sd[i])
                clipObj.put(key, JSONArray().apply {
                    put(lo.toDouble())
                    put(hi.toDouble())
                })
                calibMeansObj.put(key, mu[i].toDouble())
            }
            root.put("clip_bounds", clipObj)
            root.put("calib_means", calibMeansObj)


            // ─── 5) store calib_means if still needed ─────────
            FEAT_NAMES.forEachIndexed { i, key ->
                calibMeansObj.put(key, mu[i].toDouble())
            }
            root.put("calib_means", calibMeansObj)

            /* clip bounds: percentile 5–95% fallback */
            FEAT_NAMES.forEachIndexed { i, key ->
                val m = mu[i]; val s = sd[i]
                val lo = if (s > 0) m - 3 * s else m * 0.5f
                val hi = if (s > 0) m + 3 * s else m * 1.5f
                clipObj.put(key, JSONArray().apply { put(lo.toDouble()); put(hi.toDouble()) })
            }
            root.put("clip_bounds", clipObj)

            /* minimal stats subset for rule */
            root.put("stats", JSONObject().apply {
                put("hr_mean", JSONObject().apply { put("mu", mu[2].toDouble()); put("sigma", sd[2].toDouble()) })
                put("pnn50",   JSONObject().apply { put("mu", mu[0].toDouble()); put("sigma", sd[0].toDouble()) })
                put("kurtosis",JSONObject().apply { put("mu", mu[8].toDouble()); put("sigma", sd[8].toDouble()) })
            })
        }
        file.writeText(root.toString())
    }

    /* ───────────── raw green μ/σ loader ───────────── */
    private fun loadRawStats() {
        try {
            val fp = File(filesDir, "calib.json"); if (!fp.exists()) return
            JSONObject(fp.readText()).optJSONObject("stats_raw")?.optJSONObject("green")?.let {
                gMu = it.optDouble("mu", 0.0).toFloat()
                gSigma = it.optDouble("sigma", 1.0).toFloat()
            }
        } catch (_: Exception) {}
    }

    /* ───────────── Notification helpers ───────────── */
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(NOTI_CH, "PPG CalibRule", NotificationManager.IMPORTANCE_LOW))
        }
    }
    private fun notif(msg: String) = NotificationCompat.Builder(this, NOTI_CH)
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setContentTitle("PPG")
        .setContentText(msg)
        .build()

    /* ───────────── life‑cycle ───────────── */
    override fun onDestroy() { super.onDestroy(); stopSensors(); scope.cancel() }
}
