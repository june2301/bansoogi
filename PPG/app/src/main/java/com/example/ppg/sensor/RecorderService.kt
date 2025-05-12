/*
 * RecorderService.kt – Compile errors fixed
 *
 * 수정 사항:
 * 1. PpgSet 클래스 잘못 import된 부분 수정
 * 2. DataPoint를 직접 사용하고 timestamp/getValue() 호출
 * 3. ValueKey.PpgSet 키 사용
 */
package com.example.ppg.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.PpgType
import com.samsung.android.service.health.tracking.data.ValueKey
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Objects

class RecorderService : Service() {
    // Coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Samsung SDK
    private lateinit var sdk: HealthTrackingService
    private var tracker: HealthTracker? = null

    // Buffers
    private val ts = mutableListOf<Long>()
    private val g = mutableListOf<Int>()
    private val r = mutableListOf<Int>()
    private val ir = mutableListOf<Int>()

    // State
    private var label = "unknown"
    private var startMs = 0L
    private var shouldRecord = false

    // Tracker listener
    private val trackerListener =
        object : HealthTracker.TrackerEventListener {
            private var cnt = 0

            override fun onDataReceived(data: List<DataPoint>) {
                data.forEach { dp: DataPoint ->
                    val timestamp = dp.timestamp
                    Log.d("Recorder", "Timestamp: $timestamp")
                    ts.add(timestamp)
                    println(dp.toString())
//                    dp.getValue<>()
                    val greenArr = dp.getValue(ValueKey.PpgSet.PPG_GREEN) as? Objects
                    val redArr = dp.getValue(ValueKey.PpgSet.PPG_RED) as? Objects
                    val irArr = dp.getValue(ValueKey.PpgSet.PPG_IR) as? Objects

                    println("$greenArr")
//                    Log.d("Recorder", "PPG_GREEN: ${greenArr?.contentToString()}")
//                    Log.d("Recorder", "PPG_RED: ${redArr?.contentToString()}")
//                    Log.d("Recorder", "PPG_IR: ${irArr?.contentToString()}")
//
//                    greenArr?.let { g.addAll(it.map { f -> f.toInt() }) }
//                    redArr?.let { r.addAll(it.map { f -> f.toInt() }) }
//                    irArr?.let { ir.addAll(it.map { f -> f.toInt() }) }

                    if (++cnt % 25 == 0) Log.d("Recorder", "Buffered $cnt samples")
                }
            }

            override fun onFlushCompleted() {}

            override fun onError(error: HealthTracker.TrackerError) {
                Log.e("Recorder", "PPG error: $error")
            }
        }

    // Connection listener
    private val connListener =
        object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i("Recorder", "SDK connected")
                tracker =
                    sdk.getHealthTracker(
                        HealthTrackerType.PPG_CONTINUOUS,
                        setOf(PpgType.GREEN, PpgType.RED, PpgType.IR),
                    )
                startTrackingIfReady()
            }

            override fun onConnectionEnded() {
                Log.i("Recorder", "SDK disconnected")
            }

            override fun onConnectionFailed(e: HealthTrackerException) {
                Log.e("Recorder", "Connect failed: ${e.errorCode}")
            }
        }

    companion object {
        private const val CHANNEL_ID = "PPG"
        private const val NOTI_ID = 1
        const val ACTION_START = "com.example.ppg.START"
        const val ACTION_STOP = "com.example.ppg.STOP"
    }

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
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                label = intent.getStringExtra("label") ?: "unknown"
                startMs = System.currentTimeMillis()
                shouldRecord = true
                startForeground(NOTI_ID, notif("Preparing…"))
                scope.launch { initSdk() }
            }
            ACTION_STOP -> {
                shouldRecord = false
                scope.launch { stopAndSave() }
            }
        }
        return START_STICKY
    }

    private suspend fun initSdk() {
        if (::sdk.isInitialized) {
            startTrackingIfReady()
            return
        }
        withContext(Dispatchers.Main) {
            sdk = HealthTrackingService(connListener, this@RecorderService)
            sdk.connectService()
        }
    }

    private fun startTrackingIfReady() {
        if (!shouldRecord || tracker == null) return
        tracker!!.setEventListener(trackerListener)
        try {
            tracker!!.javaClass.getMethod("start").invoke(tracker)
        } catch (_: NoSuchMethodException) {
        }
        startForeground(NOTI_ID, notif("Recording $label"))
    }

    private suspend fun stopAndSave() {
        tracker?.unsetEventListener()
        runCatching { tracker?.javaClass?.getMethod("stop")?.invoke(tracker) }
        if (::sdk.isInitialized) sdk.disconnectService()
        stopForeground(STOP_FOREGROUND_REMOVE)
        saveToJson()
        stopSelf()
    }

    // Notification
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "PPG recorder", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    private fun notif(text: String): Notification =
        (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID)
            } else {
                Notification.Builder(this)
            }
        ).setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("PPG Recorder")
            .setContentText(text)
            .build()

    // File I/O
    private suspend fun saveToJson() =
        withContext(Dispatchers.IO) {
            val duration = (System.currentTimeMillis() - startMs) / 1000
            val root =
                JSONObject().apply {
                    put("label", label)
                    put("start_ts", startMs)
                    put("duration_s", duration)
                    put("sampling", JSONObject().put("ppg", 25))
                    put(
                        "data",
                        JSONObject().apply {
                            put("ts", JSONArray(ts))
                            put("ppg_green", JSONArray(g))
                            put("ppg_red", JSONArray(r))
                            put("ppg_ir", JSONArray(ir))
                        },
                    )
                }
            File(filesDir, "recordings").apply { mkdirs() }.let { dir ->
                val safe = label.replace("[\\/:*?\"<>| ]".toRegex(), "-")
                val file = File(dir, "${startMs}_$safe.json")
                file.writeText(root.toString())
                Log.i("Recorder", "Saved to ${file.absolutePath}")
            }
        }
}
