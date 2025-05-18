/*
 * RecorderService.kt ― Galaxy Watch Foreground‑Service
 * ----------------------------------------------------
 * • Samsung Health Tracking SDK 1.3 – PPG_CONTINUOUS 25 Hz
 * • 1 초(25 frame)마다 HRV‑10 feature  → 10 초 이동창(90 % overlap) 평균 → TFLite 추론
 * • ACTION_PREDICTION 브로드캐스트로 UI 전달
 * • raw PPG + 예측 목록을 /files/recordings/YYYYMMDD_HHMMSS_label.json 저장
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class RecorderService : Service() {

    /* ---------- 상수 ---------- */
    private val FS              = 25          // Hz
    private val WINDOW_SEC      = 10          // 10 초 분석 윈도우
    private val INFER_INTERVAL  = 1           // 1 초마다 추론

    /* ---------- 상태 ---------- */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recording  = false
    private var warmupDone = false

    private lateinit var hts: HealthTrackingService
    private lateinit var ppgTracker: HealthTracker
    private lateinit var classifier: PostureClassifier

    private var label     = "unknown"
    private var modelFile = "ppg_10s_0.tflite"
    private var startMs   = 0L

    /* ---------- RAW 버퍼 ---------- */
    private val tsBuf = mutableListOf<Long>()      // JSON 저장용
    private val gBuf  = mutableListOf<Int>()
    private val rBuf  = mutableListOf<Int>()
    private val irBuf = mutableListOf<Int>()

    /** 최근 10 초(= 250 샘플) 원형 버퍼 */
    private val rawGBuf = ArrayDeque<Float>(FS * WINDOW_SEC)

    /* ---------- 예측 버퍼 ---------- */
    private val predList = mutableListOf<Int>()
    private var frameCnt = 0                       // 1 초 카운터

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
                delay(5_000)               // 5 초 워밍업
                warmupDone = true
                startMs = System.currentTimeMillis()
                Log.i(TAG, "Warm‑up complete")
            }
        }
        override fun onConnectionFailed(e: HealthTrackerException) {
            Log.e(TAG, "SDK connect fail: ${e.errorCode}"); stopSelf()
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
                val ts   = dp.timestamp
                val gRaw = dp.getValue(ValueKey.PpgSet.PPG_GREEN) as? Int ?: return
                val rRaw = dp.getValue(ValueKey.PpgSet.PPG_RED)   as? Int ?: return
                val irRaw= dp.getValue(ValueKey.PpgSet.PPG_IR)    as? Int ?: return

                /* ---- raw 저장 (파일용) ---- */
                tsBuf += ts; gBuf += gRaw; rBuf += rRaw; irBuf += irRaw

                /* ---- 10 초 버퍼 유지 ---- */
                rawGBuf.addLast(gRaw / 4096f)          // float 정규화
                if (rawGBuf.size > FS * WINDOW_SEC) rawGBuf.removeFirst()

                /* ---- 1 초마다 추론 ---- */
                frameCnt++
                if (frameCnt < FS * INFER_INTERVAL) return@forEach
                frameCnt = 0

                if (rawGBuf.size == FS * WINDOW_SEC) {
                    val feats = extractFeatures10s(rawGBuf.toFloatArray())
                    val pred  = classifier.classify(feats)
                    predList += pred

                    sendBroadcast(
                        Intent(ACTION_PREDICTION)
                            .setPackage(packageName)
                            .putExtra("prediction", pred)
                    )
                }
            }
        }
        override fun onFlushCompleted() {}
        override fun onError(e: HealthTracker.TrackerError) {
            Log.e(TAG, "PPG error=$e")
        }
    }

    /* =========================================================
     *  신호 전처리 : detrend → zero‑phase Butterworth(0.5–5 Hz)
     * ======================================================= */
    /** 선형 추세 제거 */
    private fun detrend(x: FloatArray): FloatArray {
        val n = x.size
        val xs = DoubleArray(n) { it.toDouble() }
        val ys = x.map { it.toDouble() }.toDoubleArray()
        val xBar = xs.average(); val yBar = ys.average()
        val slope = xs.zip(ys).sumOf { (xi, yi) -> (xi - xBar)*(yi - yBar) } /
                xs.sumOf { (it - xBar).pow(2) }
        val intercept = yBar - slope * xBar
        return FloatArray(n) { i -> (x[i] - (slope*i + intercept)).toFloat() }
    }

    /** zero‑phase 2‑pole 0.5–5 Hz band‑pass (fs = 25 Hz) */
    private val b = floatArrayOf(0.20657208f, 0f, -0.41314417f, 0f, 0.20657208f)
    private val a = floatArrayOf(1f, -0.36952738f, -0.19581571f, 0.01657282f, 0.03646653f)
    private fun zeroPhaseFilter(sig: FloatArray): FloatArray {
        /* forward */
        val fwd = FloatArray(sig.size); var z1 = 0f; var z2 = 0f
        for (i in sig.indices) {
            val y = b[0]*sig[i] + z1
            z1 = b[1]*sig[i] - a[1]*y + z2
            z2 = b[2]*sig[i] - a[2]*y
            fwd[i] = y
        }
        /* reverse */
        val out = FloatArray(sig.size); z1 = 0f; z2 = 0f
        for (j in fwd.indices.reversed()) {
            val y = b[0]*fwd[j] + z1
            z1 = b[1]*fwd[j] - a[1]*y + z2
            z2 = b[2]*fwd[j] - a[2]*y
            out[j] = y
        }
        return out
    }

    /* =========================================================
     *  HRV‑10 feature (train_ppg_model.py 와 동일)
     * ======================================================= */
    private fun extractFeatures10s(raw: FloatArray): FloatArray {
        val x = zeroPhaseFilter(detrend(raw))

        /* ---- peak & trough ---- */
        val peaks = mutableListOf<Int>(); val tr = mutableListOf<Int>()
        for (i in 1 until x.lastIndex) {
            if (x[i] > x[i-1] && x[i] > x[i+1]) peaks += i
            if (x[i] < x[i-1] && x[i] < x[i+1]) tr    += i
        }

        /* ---- RR / HRV ---- */
        val rr = peaks.zipWithNext { a,b -> (b-a).toFloat()/FS }
        val out = FloatArray(10) { 0f }
        if (rr.isNotEmpty()) {
            out[4] = peaks.size.toFloat()              // n_peaks
            val rrMean = rr.average().toFloat()
            out[1] = rrMean                            // rr_mean
            out[2] = if (rrMean>0f) 60f/rrMean else 0f // hr_mean
        }
        if (rr.size > 1) {
            val diffs = rr.zipWithNext { a,b -> b-a }
            out[3] = sqrt(diffs.map{it*it}.average()).toFloat()     // rmssd
            out[0] = diffs.count{ abs(it)>0.05f }.toFloat() / diffs.size  // pnn50
        }

        /* ---- crest / dwell / pwtf ---- */
        val cyc = peaks.mapNotNull { p ->
            val f = tr.filter{it<p}.maxOrNull() ?: return@mapNotNull null
            val n = tr.filter{it>p}.minOrNull() ?: return@mapNotNull null
            Triple(f,p,n)
        }
        if (cyc.isNotEmpty()) {
            val ct = cyc.map{ (f,p,_) -> (p-f).toFloat()/FS }.average().toFloat()
            val dw = cyc.map{ (f,_,n) -> (n-f).toFloat()/FS }.average().toFloat()
            out[5] = ct; out[6] = dw; out[7] = if (dw!=0f) ct/dw else 0f
        }

        /* ---- kurtosis & skewness ---- */
        val mean = x.average().toFloat()
        val m2 = x.map{ (it-mean).pow(2) }.average().toFloat()
        val m3 = x.map{ (it-mean).pow(3) }.average().toFloat()
        val m4 = x.map{ (it-mean).pow(4) }.average().toFloat()
        out[8] = if (m2>1e-6) m4/(m2*m2) else 0f
        out[9] = if (m2>1e-6) m3/(m2.pow(1.5f)) else 0f

        return out
    }

    /* =========================================================
     *  Service‑lifecycle
     * ======================================================= */
    override fun onCreate() { super.onCreate(); createChannel() }
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    override fun onStartCommand(i: Intent?, flags: Int, id: Int): Int {
        when (i?.action) {
            ACTION_START -> {
                label     = i.getStringExtra("label") ?: label
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
    private fun startTracker() =
        runCatching { ppgTracker.javaClass.getMethod("start").invoke(ppgTracker) }

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
    private fun notif(msg: String): Notification =
        NotificationCompat.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("PPG Recorder")
            .setContentText(msg)
            .build()

    /* =========================================================
     *  JSON 직렬화
     * ======================================================= */
    private suspend fun saveJson() = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileName = "${sdf.format(Date(startMs))}_${label.replace("[^\\w]".toRegex(), "_")}.json"

        val root = JSONObject().apply {
            put("label", label)
            put("model", modelFile)
            put("start_ts", startMs)
            put("duration_s", ((System.currentTimeMillis() - startMs) / 1000.0).roundToInt())
            put("data", JSONObject().apply {
                put("ppg_continuous", JSONObject().apply {
                    put("ts",    JSONArray(tsBuf))
                    put("green", JSONArray(gBuf))
                    put("red",   JSONArray(rBuf))
                    put("ir",    JSONArray(irBuf))
                })
            })
            put("predictions", JSONArray(predList))
        }
        File(File(filesDir, "recordings").apply { mkdirs() }, fileName)
            .writeText(root.toString())
    }

    /* ========================================================= */
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
