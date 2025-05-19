/*
 * RecorderService.kt — Galaxy Watch Foreground‑Service (fixed)
 * ------------------------------------------------------------
 * • Samsung Health Tracking SDK 1.3 – PPG_CONTINUOUS 25 Hz
 * • 1 초(25 frame)마다 HRV‑10 feature  → 10 초 이동창(90 % overlap) 평균 → TFLite 추론
 * • ACTION_PREDICTION 브로드캐스트로 UI 전달
 * • raw PPG + 예측 목록을 /files/recordings/YYYYMMDD_HHMMSS_label.json 저장
 *
 *   🔧 2025‑05‑19 patch
 *   ────────────────────────────────────────────────────────────
 *   1) Butterworth 계수를 훈련 파이프라인 값과 동일하게 교체
 *   2) 필터를 filtfiltOnce × 2 패스로 구현 → 8‑pole zero‑phase
 *   3) peaks distance 필터를 1 회만 적용해 Python find_peaks 와 동기화
 *   4) loadRawStats 중 괄호 오류 수정
 */
package com.example.ppg.sensor

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ppg.inference.PostureClassifier
import com.samsung.android.service.health.tracking.*
import com.samsung.android.service.health.tracking.data.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class RecorderService : Service() {

    /* ---------- 상수 ---------- */
    private val FS         = 25          // Hz
    private val WINDOW_SEC = 10          // 분석 윈도우 길이(초)
    private val MIN_DIST   = (FS * 0.4).toInt() // find_peaks distance

    // raw green 채널 전처리용 μ/σ (calib.json → stats_raw.green)
    private var rawGreenMu    = 0f
    private var rawGreenSigma = 1f

    /* ---------- 상태 ---------- */
    private val scope      = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recording  = false
    private var warmupDone = false

    private lateinit var hts: HealthTrackingService
    private lateinit var ppgTracker: HealthTracker
    private lateinit var classifier: PostureClassifier

    private var label     = "unknown"
    private var modelFile = "ppg_10s_0.tflite"
    private var startMs   = 0L

    /* ---------- RAW 버퍼 ---------- */
    private val tsBuf = mutableListOf<Long>()
    private val gBuf  = mutableListOf<Int>()
    private val rBuf  = mutableListOf<Int>()
    private val irBuf = mutableListOf<Int>()
    private val rawGBuf = ArrayDeque<Float>(FS * WINDOW_SEC) // 최근 10초(250샘플)

    /* ---------- 예측 버퍼 ---------- */
    private val predList = mutableListOf<Int>()
    private var frameCnt = 0

    /* =========================================================
     *  SDK 연결
     * ======================================================= */
    private val connListener = object : ConnectionListener {
        override fun onConnectionSuccess() {
            Log.i(TAG, "SDK connected")
            ppgTracker = hts.getHealthTracker(
                HealthTrackerType.PPG_CONTINUOUS,
                setOf(PpgType.GREEN, PpgType.RED, PpgType.IR)
            )
            ppgTracker.setEventListener(ppgListener)
            startTracker()
            scope.launch {
                delay(5_000)
                warmupDone = true
                startMs = System.currentTimeMillis()
                Log.i(TAG, "Warm‑up complete")
            }
        }
        override fun onConnectionFailed(e: HealthTrackerException) {
            Log.e(TAG, "SDK connect fail: ${e.errorCode}")
            stopSelf()
        }
        override fun onConnectionEnded() {
            Log.i(TAG, "SDK disconnected")
        }
    }

    /* =========================================================
     *  PPG LISTENER (25 Hz)
     * ======================================================= */
    private val ppgListener = object : HealthTracker.TrackerEventListener {
        override fun onDataReceived(points: List<DataPoint>) {
            if (!recording || !warmupDone) return
            points.forEach { dp ->
                val ts = dp.timestamp
                val gRaw = (dp.getValue(ValueKey.PpgSet.PPG_GREEN) as? Number)?.toLong() ?: return
                val rRaw = (dp.getValue(ValueKey.PpgSet.PPG_RED)   as? Number)?.toLong() ?: return
                val irRaw= (dp.getValue(ValueKey.PpgSet.PPG_IR)    as? Number)?.toLong() ?: return

                tsBuf += ts
                gBuf  += gRaw.toInt()
                rBuf  += rRaw.toInt()
                irBuf += irRaw.toInt()

                // -------- 이동창 관리 --------
                val gFloat = gRaw.toFloat() / 4096f
                rawGBuf.addLast(gFloat)
                if (rawGBuf.size > FS * WINDOW_SEC) rawGBuf.removeFirst()

                if (++frameCnt < FS) return@forEach // 1초마다 추론
                frameCnt = 0

                if (rawGBuf.size == FS * WINDOW_SEC) {
                    val seg = rawGBuf.toFloatArray()
                    val feats = extractFeatures10s(seg)
                    val pred  = classifier.classify(feats)
                    predList += pred
                    sendBroadcast(Intent(ACTION_PREDICTION).setPackage(packageName).putExtra("prediction", pred))
                }
            }
        }
        override fun onFlushCompleted() {}
        override fun onError(e: HealthTracker.TrackerError) {
            Log.e(TAG, "PPG error=$e")
        }
    }

    /* =========================================================
     *  전처리: detrend → 8‑pole zero‑phase Butterworth(0.5–5 Hz)
     * ======================================================= */
    private fun detrend(x: FloatArray): FloatArray {
        val n = x.size
        val xs = FloatArray(n) { it.toFloat() }
        val xBar = xs.average().toFloat()
        val yBar = x.average().toFloat()
        var cov = 0f; var varX = 0f
        for (i in 0 until n) {
            val dx = xs[i] - xBar
            val dy = x[i] - yBar
            cov   += dx * dy
            varX  += dx * dx
        }
        val slope = if (varX > 0f) cov / varX else 0f
        val intercept = yBar - slope * xBar
        return FloatArray(n) { i -> x[i] - (slope * xs[i] + intercept) }
    }

    // Butterworth(0.5–5 Hz, fs=25 Hz) 계수 — float32 반올림
    private val b = floatArrayOf(0.17508765f, 0f, -0.3501753f, 0f, 0.17508765f)
    private val a = floatArrayOf(1.0f, -2.2990553f, 1.9674978f, -0.87480557f, 0.21965398f)

    /** forward+reverse(4‑pole) 1 회 수행 */
    private fun filtfiltOnce(sig: FloatArray): FloatArray {
        val fwd = FloatArray(sig.size)
        var z1 = 0f; var z2 = 0f
        for (i in sig.indices) {
            val y = b[0] * sig[i] + z1
            z1 = b[1] * sig[i] - a[1] * y + z2
            z2 = b[2] * sig[i] - a[2] * y
            fwd[i] = y
        }
        val out = FloatArray(sig.size)
        z1 = 0f; z2 = 0f
        for (j in fwd.indices.reversed()) {
            val y = b[0] * fwd[j] + z1
            z1 = b[1] * fwd[j] - a[1] * y + z2
            z2 = b[2] * fwd[j] - a[2] * y
            out[j] = y
        }
        return out
    }

    /** 8‑pole zero‑phase Butterworth (filtfiltOnce ×2) */
    private fun zeroPhaseFilter(sig: FloatArray): FloatArray = filtfiltOnce(filtfiltOnce(sig))

    /* =========================================================
     *  HRV‑10 feature (exactly train_ppg_model_with_calib.py)
     * ======================================================= */
    private fun extractFeatures10s(raw: FloatArray): FloatArray {
        val detrended = detrend(raw)
        val normed = FloatArray(detrended.size) { i ->
            if (rawGreenSigma > 0f) (detrended[i] - rawGreenMu) / rawGreenSigma else detrended[i] - rawGreenMu
        }
        val x = zeroPhaseFilter(normed)

        /* ---- peak & trough ---- */
        val rawPeaks = mutableListOf<Int>()
        val rawTroughs = mutableListOf<Int>()
        for (i in 1 until x.lastIndex) {
            if (x[i] > x[i-1] && x[i] > x[i+1]) rawPeaks += i
            if (x[i] < x[i-1] && x[i] < x[i+1]) rawTroughs += i
        }
        val peaks = mutableListOf<Int>()
        var lastP = -MIN_DIST
        rawPeaks.forEach { p -> if (p - lastP >= MIN_DIST) { peaks += p; lastP = p } }

        val troughs = mutableListOf<Int>()
        var lastT = -MIN_DIST
        rawTroughs.forEach { t -> if (t - lastT >= MIN_DIST) { troughs += t; lastT = t } }

        /* ---- RR / HRV ---- */
        val rr = peaks.zipWithNext { a, b -> (b - a).toFloat() / FS }
        val out = FloatArray(10)
        if (rr.isNotEmpty()) {
            out[4] = peaks.size.toFloat() // n_peaks
            val rrMean = rr.average().toFloat()
            out[1] = rrMean              // rr_mean
            out[2] = if (rrMean > 0f) 60f / rrMean else 0f // hr_mean
        }
        if (rr.size > 1) {
            val diffs = rr.zipWithNext { a, b -> b - a }
            out[3] = sqrt(diffs.map { it * it }.average()).toFloat()         // rmssd
            out[0] = diffs.count { abs(it) > 0.05f }.toFloat() / diffs.size  // pnn50
        }

        /* ---- crest / dwell / pwtf ---- */
        val cycles = peaks.mapNotNull { p ->
            val f = troughs.lastOrNull { it < p } ?: return@mapNotNull null
            val n = troughs.firstOrNull { it > p } ?: return@mapNotNull null
            Triple(f, p, n)
        }
        if (cycles.isNotEmpty()) {
            val ct = cycles.map { (f, p, _) -> (p - f).toFloat() / FS }.average().toFloat()
            val dw = cycles.map { (f, _, n) -> (n - f).toFloat() / FS }.average().toFloat()
            out[5] = ct; out[6] = dw; out[7] = if (dw != 0f) ct / dw else 0f
        }

        /* ---- kurtosis & skewness ---- */
        val mean = x.average().toFloat()
        val m2 = x.map { (it - mean).pow(2) }.average().toFloat()
        val m3 = x.map { (it - mean).pow(3) }.average().toFloat()
        val m4 = x.map { (it - mean).pow(4) }.average().toFloat()
        out[8] = if (m2 > 1e-6f) (m4 / (m2 * m2) - 3f) else 0f // excess kurtosis
        out[9] = if (m2 > 1e-6f) (m3 / m2.pow(1.5f)) else 0f   // skewness
        return out
    }

    /* =========================================================
     *  calib.json 유틸
     * ======================================================= */
    private fun ensureCalibrationFile() {
        val outFile = File(filesDir, "calib.json")
        if (outFile.exists()) return
        runCatching {
            assets.open("calib.json").use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
        }.onFailure { Log.w(TAG, "Failed to copy calib.json", it) }
    }

    private fun loadRawStats() {
        val file = File(filesDir, "calib.json")
        if (!file.exists()) return
        try {
            JSONObject(file.readText())
                .optJSONObject("stats_raw")
                ?.optJSONObject("green")
                ?.let { g ->
                    rawGreenMu    = g.optDouble("mu", 0.0).toFloat()
                    rawGreenSigma = g.optDouble("sigma", 1.0).toFloat()
                }
        } catch (_: Exception){}
    }

    /* ========================================================= */
    override fun onCreate() { super.onCreate(); createChannel() }
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    override fun onStartCommand(i: Intent?, flags: Int, id: Int): Int {
        when (i?.action) {
            ACTION_START -> {
                ensureCalibrationFile(); loadRawStats()
                label = i.getStringExtra("label") ?: label
                modelFile = i.getStringExtra("model") ?: modelFile
                classifier = PostureClassifier(this, modelFile)
                recording = true
                startForeground(NOTI_ID, notif("Preparing…"))
                scope.launch { connectSdk() }
            }
            ACTION_STOP -> {
                recording = false
                scope.launch { stopAndSave() }
            }
        }
        return START_STICKY
    }

    private suspend fun connectSdk() = withContext(Dispatchers.Main) {
        hts = HealthTrackingService(connListener, this@RecorderService)
        hts.connectService()
    }

    private fun startTracker() = runCatching {
        ppgTracker.javaClass.getMethod("start").invoke(ppgTracker)
    }

    /* =========================================================
     *  STOP → JSON 저장
     * ======================================================= */
    private suspend fun stopAndSave() {
        runCatching {
            ppgTracker.unsetEventListener()
            ppgTracker.javaClass.getMethod("stop").invoke(ppgTracker)
        }
        if (::hts.isInitialized) hts.disconnectService()
        saveJson()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /* =========================================================
     *  Notification helpers
     * ======================================================= */
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CH, "PPG", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun notif(msg: String): Notification = NotificationCompat.Builder(this, CH)
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setContentTitle("PPG Recorder")
        .setContentText(msg)
        .build()

    /* =========================================================
     *  JSON 직렬화
     * ======================================================= */
    private suspend fun saveJson() = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fname = "${sdf.format(Date(startMs))}_${label.replace("[^\\w]".toRegex(), "_")}.json"
        val root = JSONObject().apply {
            put("label", label)
            put("model", modelFile)
            put("start_ts", startMs)
            put("duration_s", ((System.currentTimeMillis() - startMs) / 1000.0).roundToInt())
            put("data", JSONObject().apply {
                put("ppg_continuous", JSONObject().apply {
                    put("ts", JSONArray(tsBuf))
                    put("green", JSONArray(gBuf))
                    put("red", JSONArray(rBuf))
                    put("ir", JSONArray(irBuf))
                })
            })
            put("predictions", JSONArray(predList))
        }
        File(File(filesDir, "recordings").apply { mkdirs() }, fname).writeText(root.toString())
    }

    companion object {
        private const val TAG = "RecorderSvc"
        private const val CH  = "PPG_CH"
        private const val NOTI_ID = 1
        const val ACTION_START      = "com.example.ppg.START"
        const val ACTION_STOP       = "com.example.ppg.STOP"
        const val ACTION_PREDICTION = "com.example.ppg.PREDICTION"
    }
}


/* ---------------- extension helpers ---------------- */
private fun List<Float>.averageOrNull(): Double? =
    if (isNotEmpty()) average() else null
