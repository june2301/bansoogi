package com.example.ppg.sensor

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.samsung.android.service.health.tracking.*
import com.samsung.android.service.health.tracking.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class CalibRuleService : Service() {
    companion object {
        const val ACTION_CALIB      = "com.example.ppg.CALIB"
        const val ACTION_LIVE       = "com.example.ppg.LIVE"
        const val ACTION_STOP       = "com.example.ppg.STOP"
        const val ACTION_RULE       = "com.example.ppg.RULE"
        const val ACTION_CALIB_DONE = "com.example.ppg.CALIB_DONE"
        private const val TAG = "CalibRuleSvc"
    }

    /* --- constants --- */
    private val FS = 25; private val WIN_SEC = 10; private val INFER_EVERY = 1
    private val NOTI_CH = "CALIB_RULE_CH"; private val NOTI_ID = 2

    /* --- state --- */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var hts: HealthTrackingService
    private lateinit var ppgTracker: HealthTracker
    private var recording = false; private var liveMode = false
    private var warmupDone = false; private var frameCnt = 0
    private val rawBuf = ArrayDeque<Float>()

    private val calibLabelMutex = Mutex()
    private var currentLabel: String = "unknown"
    private val calibMap = mutableMapOf<String, MutableList<FloatArray>>()

    /* ========================================================= */
    override fun onCreate() { super.onCreate(); createChannel() }
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, id: Int): Int {
        when (i?.action) {
            ACTION_CALIB -> startCalibration(i.getStringExtra("label") ?: "unknown")
            ACTION_LIVE  -> startLive()
            ACTION_STOP  -> stopSelf()
        }
        return START_STICKY
    }

    private fun startCalibration(label: String) {
        liveMode = false; currentLabel = label; prepareSensors()
        startForeground(NOTI_ID, notif("Calibrating $label…"))
        scope.launch {
            val CALIB_DURATION = 35_000L
            delay(CALIB_DURATION)
            stopSensors()
            writeCalibJson()
            sendBroadcast(Intent(ACTION_CALIB_DONE).setPackage(packageName).putExtra("label", label))
            stopSelf()
        }
    }

    private fun startLive() {
        liveMode = true; prepareSensors()
        startForeground(NOTI_ID, notif("Rule‑based Inference"))
    }

    private fun prepareSensors() {
        if (recording) return
        recording = true; warmupDone = false; frameCnt = 0; rawBuf.clear()
        hts = HealthTrackingService(conn, this)
        hts.connectService()
    }

    private fun stopSensors() {
        recording = false
        runCatching { ppgTracker.unsetEventListener() }
        runCatching { hts.disconnectService() }
    }

    /* ---------- Samsung SDK Callbacks ---------- */
    private val conn = object : ConnectionListener {
        override fun onConnectionSuccess() {
            ppgTracker = hts.getHealthTracker(HealthTrackerType.PPG_CONTINUOUS, setOf(PpgType.GREEN))
            ppgTracker.setEventListener(ppgListener)
            startTracker()
            scope.launch { delay(5_000); warmupDone = true }
        }
        override fun onConnectionFailed(e: HealthTrackerException) {
            Log.e(TAG, "connect fail ${e.errorCode}"); stopSelf()
        }
        override fun onConnectionEnded() {}
    }

    private val ppgListener = object : HealthTracker.TrackerEventListener {
        override fun onDataReceived(points: List<DataPoint>) {
            if (!recording || !warmupDone) return
            points.forEach { dp ->
                val g = (dp.getValue(ValueKey.PpgSet.PPG_GREEN) as? Int ?: 0) / 4096f
                rawBuf.addLast(g); if (rawBuf.size > FS*WIN_SEC) rawBuf.removeFirst()
                if (++frameCnt < FS*INFER_EVERY) return@forEach; frameCnt = 0
                if (rawBuf.size == FS*WIN_SEC) handleWindow(extractFeatures(rawBuf.toFloatArray()))
            }
        }
        override fun onFlushCompleted() {}
        override fun onError(e: HealthTracker.TrackerError) { Log.e(TAG, "PPG error $e") }
    }

    private fun handleWindow(f: FloatArray) {
        if (liveMode) sendRuleBroadcast(calcRule(f))
        else scope.launch { calibLabelMutex.withLock {
            calibMap.getOrPut(currentLabel) { mutableListOf() }.add(f)
        }}
    }

    /* ---------- Feature extraction (short, HRV subset) ---------- */
    private fun extractFeatures(x: FloatArray): FloatArray {
        // simple peak‑based HR metrics only (indices align with training script positions)
        val peaks = mutableListOf<Int>()
        for (i in 1 until x.lastIndex) if (x[i] > x[i-1] && x[i] > x[i+1]) peaks += i
        val rr = peaks.zipWithNext { a,b -> (b-a).toFloat()/FS }
        val out = FloatArray(10)
        if (rr.isNotEmpty()) {
            val mean = rr.average().toFloat()
            out[1] = mean; out[2] = if (mean>0f) 60f/mean else 0f; out[4] = peaks.size.toFloat()
        }
        if (rr.size>1) {
            val diff = rr.zipWithNext{a,b->b-a};
            out[3] = sqrt(diff.map{it*it}.average()).toFloat()
            out[0] = diff.count{ abs(it) > 0.05 }.toFloat()/diff.size
        }
        return out
    }
    private fun startTracker() =
        runCatching { ppgTracker.javaClass.getMethod("start").invoke(ppgTracker) }

    /* ---------- Rule logic using calib.json ---------- */
    private fun calcRule(f: FloatArray): String {
        val file = File(filesDir, "calib.json"); if (!file.exists()) return "upright‑sitting"
        val root = JSONObject(file.readText())
        val stats = root.optJSONObject("stats") ?: return "upright‑sitting"
        fun z(value: Float, key: String): Float {
            val s = stats.optJSONObject(key) ?: return 0f
            val mu = s.optDouble("mu",0.0).toFloat(); val sd = s.optDouble("sigma",1.0).toFloat()
            return if (sd != 0f) (value-mu)/sd else 0f
        }
        val hrZ = z(f[2], "hr_mean"); val pnnZ = z(f[0], "pnn50"); val kurZ = z(f[8], "kurtosis")
        val thr = root.optJSONObject("thresholds") ?: return "upright‑sitting"
        val sup = thr.optJSONObject("supine"); val std = thr.optJSONObject("standing")
        return when {
            sup != null && hrZ < sup.optDouble("hr_z_max",-999.0).toFloat() && pnnZ > sup.optDouble("pnn50_z_min",0.0).toFloat() -> "supine‑lying"
            std != null && hrZ > std.optDouble("hr_z_min",999.0).toFloat() && kurZ > std.optDouble("kurtosis_z_min",0.0).toFloat() -> "standing"
            else -> "upright‑sitting"
        }
    }

    private fun sendRuleBroadcast(label: String) {
        sendBroadcast(Intent(ACTION_RULE).setPackage(packageName).putExtra("rule_label", label))
    }

    /* ---------- write calib.json ---------- */
    private suspend fun writeCalibJson() = withContext(Dispatchers.IO) {
        if (calibMap.isEmpty()) return@withContext
        val file = File(filesDir, "calib.json")
        val root = runCatching { JSONObject(file.readText()) }.getOrNull() ?: JSONObject()
        val rawObj = root.optJSONObject("raw") ?: JSONObject().also { root.put("raw", it) }

        calibMap.forEach { (lab, list) ->
            val arr = rawObj.optJSONArray(lab) ?: JSONArray().also { rawObj.put(lab, it) }
            list.forEach { window -> arr.put(JSONArray(window.toList())) }
        }
        calibMap.clear()

        // collect all windows for stats & subject mean
        val allWindows = mutableListOf<FloatArray>()
        rawObj.keys().forEach { k ->
            rawObj.optJSONArray(k)?.let { ja ->
                for (i in 0 until ja.length()) {
                    ja.optJSONArray(i)?.let { ja2 ->
                        allWindows += FloatArray(ja2.length()) { idx -> ja2.optDouble(idx, 0.0).toFloat() }
                    }
                }
            }
        }

        if (allWindows.isNotEmpty()) {
            val dims = allWindows[0].size
            val mu = FloatArray(dims)
            val sd = FloatArray(dims)
            allWindows.forEach { w -> w.forEachIndexed { i, v -> mu[i] += v } }
            val count = allWindows.size
            mu.forEachIndexed { i, v -> mu[i] = v / count }
            allWindows.forEach { w -> w.forEachIndexed { i, v -> sd[i] += (v - mu[i]).pow(2) } }
            sd.forEachIndexed { i, v -> sd[i] = sqrt(v / count) }

            // write stats
            val statsObj = JSONObject().apply {
                put("hr_mean", JSONObject().apply { put("mu", mu[2].toDouble()); put("sigma", sd[2].toDouble()) })
                put("pnn50",   JSONObject().apply { put("mu", mu[0].toDouble()); put("sigma", sd[0].toDouble()) })
                put("kurtosis",JSONObject().apply { put("mu", mu[8].toDouble()); put("sigma", sd[8].toDouble()) })
            }
            root.put("stats", statsObj)

            // write subject calibration means
            val featNames = listOf(
                "pnn50","rr_mean","hr_mean","rmssd","n_peaks",
                "crest_t","dwell_t","pwtf","kurtosis","skew"
            )
            val calibMeansObj = JSONObject().apply {
                featNames.forEachIndexed { i, key ->
                    put(key, mu[i].toDouble())
                }
            }
            root.put("calib_means", calibMeansObj)
        }

        // default thresholds
        root.put("thresholds", JSONObject().apply {
            put("supine",
                JSONObject()
                    .put("hr_z_max",    -0.8)
                    .put("pnn50_z_min",  0.8)
            )
            put("standing",
                JSONObject()
                    .put("hr_z_min",      0.8)
                    .put("kurtosis_z_min",0.5)
            )
        })

        file.writeText(root.toString())
    }

    /**
     * 2) Service 라이프사이클 오버라이드
     */

    override fun onDestroy() {
        super.onDestroy()
        stopSensors()    // 센서 해제
        scope.cancel()   // 코루틴 취소
    }


    /**
     * 3) 알림(Notification) 채널 생성 및 알림 빌더
     */
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    NOTI_CH,
                    "PPG CalibRule",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun notif(msg: String): Notification =
        NotificationCompat.Builder(this, NOTI_CH)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("PPG")
            .setContentText(msg)
            .build()
}