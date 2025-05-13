/*
 * RecorderService.kt – Samsung Health Sensor SDK v1.3.0 (PPG 전용)
 * ----------------------------------------------------------------
 * ① PPG_CONTINUOUS 25 Hz 실시간 수집
 * ② Logcat + JSON(/files/recordings/…) 저장
 * ----------------------------------------------------------------
 */
package com.example.ppg.sensor

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

/** Galaxy Watch foreground-service – PPG 전용 레코더 */
class RecorderService : Service() {
    // ---------------- Coroutine ----------------
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ---------------- Samsung SDK ----------------
    private lateinit var sdk: HealthTrackingService
    private lateinit var ppgTracker: HealthTracker // PPG 전용

    // ---------------- PPG buffers ----------------
    private val ppgTs = mutableListOf<Long>()
    private val ppgG = mutableListOf<Int>()
    private val ppgR = mutableListOf<Int>()
    private val ppgIr = mutableListOf<Int>()

    /* ---------- 주석: 다른 센서 버퍼 ----------
    private val accTs = mutableListOf<Long>()
    ...
    -------------------------------------------- */

    // ---------------- 상태 ----------------
    private var label = "unknown"
    private var startMs = 0L

    @Volatile private var recording = false

    /* ===================================================================
     * PPG Tracker listener
     * =================================================================== */
    private val ppgListener =
        object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(points: List<DataPoint>) {
                points.forEach { dp ->
                    ppgTs += dp.timestamp

                    fun Any?.toIntList(): List<Int> =
                        when (this) {
                            is Int -> listOf(this)
                            is IntArray -> this.toList()
                            else -> emptyList()
                        }

                    val g = dp.getValue(ValueKey.PpgSet.PPG_GREEN).toIntList()
                    val r = dp.getValue(ValueKey.PpgSet.PPG_RED).toIntList()
                    val ir = dp.getValue(ValueKey.PpgSet.PPG_IR).toIntList()

                    ppgG += g
                    ppgR += r
                    ppgIr += ir
                    Log.d("PPG", "${dp.timestamp}|g=${g.size} r=${r.size} ir=${ir.size}")
                }
            }

            override fun onFlushCompleted() {}

            override fun onError(error: HealthTracker.TrackerError) {
                Log.e("Recorder", "PPG error=$error")
            }
        }

    /* ===================================================================
     * SDK connection listener  (v1.3.0 API)
     * =================================================================== */
    private val connListener =
        object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i("Recorder", "SDK connected")

                // PPG_CONTINUOUS 등록
                ppgTracker =
                    sdk.getHealthTracker(
                        HealthTrackerType.PPG_CONTINUOUS,
                        setOf(PpgType.GREEN, PpgType.RED, PpgType.IR),
                    )
                ppgTracker.setEventListener(ppgListener)

                startContinuous()
            }

            /** v1.3.0 은 errorCode(Int) 하나만 전달 */
            override fun onConnectionFailed(e: HealthTrackerException) {
                Log.e("Recorder", "SDK connection failed: ${e.errorCode}")
            }

            override fun onConnectionEnded() {
                Log.i("Recorder", "SDK disconnected")
            }
        }

    // ---------------- tracker helpers ----------------
    private fun startContinuous() {
        if (!recording) return
        runCatching { ppgTracker.javaClass.getMethod("start").invoke(ppgTracker) }
        startForeground(NOTI_ID, notif("Recording $label…"))
    }

    /* ===================================================================
     * Service lifecycle
     * =================================================================== */
    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(
        i: Intent?,
        flags: Int,
        id: Int,
    ): Int {
        when (i?.action) {
            ACTION_START -> {
                label = i.getStringExtra("label") ?: "unknown"
                startMs = System.currentTimeMillis()
                recording = true
                startForeground(NOTI_ID, notif("Preparing…"))
                scope.launch { initSdk() }
            }
            ACTION_STOP -> {
                recording = false
                scope.launch { stopAndSave() }
            }
        }
        return START_STICKY
    }

    // ---------------- SDK init ----------------
    private suspend fun initSdk() {
        if (::sdk.isInitialized) {
            startContinuous()
            return
        }
        withContext(Dispatchers.Main) {
            sdk = HealthTrackingService(connListener, this@RecorderService)
            sdk.connectService()
        }
    }

    /* ===================================================================
     * STOP  →  JSON
     * =================================================================== */
    private suspend fun stopAndSave() {
        // 1) PPG tracker 종료
        runCatching {
            ppgTracker.unsetEventListener()
            ppgTracker.javaClass.getMethod("stop").invoke(ppgTracker)
        }

        /* ---------- 주석: On-demand 측정 ----------
        measureOD(HealthTrackerType.PPG_ON_DEMAND, ...)
        ------------------------------------------- */

        // 2) disconnect & 저장
        if (::sdk.isInitialized) sdk.disconnectService()
        stopForeground(STOP_FOREGROUND_REMOVE)
        saveJson()
        stopSelf()
    }

    /* ---------- 주석: On-demand 측정 함수 ----------
    private suspend fun measureOD(...) { ... }
    ----------------------------------------------- */

    /* ===================================================================
     * Notification helpers
     * =================================================================== */
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CH, "Recorder", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun notif(msg: String): Notification =
        (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CH)
            } else {
                Notification.Builder(this)
            }
        ).setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Recorder")
            .setContentText(msg)
            .build()

    /* ===================================================================
     * JSON 직렬화  (PPG 전용)
     * =================================================================== */
    private suspend fun saveJson() =
        withContext(Dispatchers.IO) {
            val dur = ((System.currentTimeMillis() - startMs) / 1000.0).roundToInt()
            val root =
                JSONObject().apply {
                    put("label", label)
                    put("start_ts", startMs)
                    put("duration_s", dur)
                    put(
                        "data",
                        JSONObject().apply {
                            put(
                                "ppg_continuous",
                                JSONObject().apply {
                                    put("ts", JSONArray(ppgTs))
                                    put("green", JSONArray(ppgG))
                                    put("red", JSONArray(ppgR))
                                    put("ir", JSONArray(ppgIr))
                                },
                            )
                        },
                    )
                }

            val dir = File(filesDir, "recordings").apply { mkdirs() }
            val safe = label.replace("[\\/:*?\"<>| ]".toRegex(), "-")
            File(dir, "${startMs}_$safe.json").writeText(root.toString())
        }

    // ---------------- const ----------------
    companion object {
        private const val CH = "REC_CHANNEL"
        private const val NOTI_ID = 1
        const val ACTION_START = "com.example.ppg.START"
        const val ACTION_STOP = "com.example.ppg.STOP"
    }
}
