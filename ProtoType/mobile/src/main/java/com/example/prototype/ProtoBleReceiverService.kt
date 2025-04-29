package com.example.prototype

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.io.InputStream
import java.nio.ByteBuffer

class ProtoBleReceiverService :
    Service(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "proto_ble_channel"
        private const val TAG = "ProtoBleReceiverService"

        // DataLayer paths
        private const val SENSOR_DATA_PATH = "/sensor_data"
        private const val ACTIVITY_UPDATE_PATH = "/activity_update"

        // LiveData for activity state
        private val _stateLiveData = MutableLiveData<ActivityState>()
        val stateLiveData: LiveData<ActivityState> = _stateLiveData
    }

    private lateinit var dataClient: DataClient
    private lateinit var messageClient: MessageClient

    override fun onCreate() {
        super.onCreate()

        // 알림 채널 생성
        createNotificationChannel()

        // 포그라운드 서비스 시작
        startForeground(NOTIFICATION_ID, createNotification())

        // Wearable API 클라이언트 초기화
        dataClient = Wearable.getDataClient(this)
        messageClient = Wearable.getMessageClient(this)

        // 리스너 등록
        dataClient.addListener(this)
        messageClient.addListener(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // 리스너 해제
        dataClient.removeListener(this)
        messageClient.removeListener(this)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channelName = "Proto BLE Service"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, channelName, importance)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("PairApp Posture Detector")
            .setContentText("Monitoring posture...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // 사용하지 않음 - MessageAPI 사용
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == SENSOR_DATA_PATH) {
            val data = messageEvent.data
            processData(data)
        } else if (messageEvent.path == ACTIVITY_UPDATE_PATH) {
            processActivityUpdate(messageEvent.data)
        }
    }

    private fun processData(data: ByteArray) {
        try {
            // 바이트 배열에서 센서 데이터 파싱
            val buffer = data.inputStream()

            // {t, ax, ay, az, gx, gy, gz, p, stepFlag} 형식으로 가정
            val timestamp = buffer.readDouble()
            val ax = buffer.readDouble()
            val ay = buffer.readDouble()
            val az = buffer.readDouble()
            val gx = buffer.readDouble()
            val gy = buffer.readDouble()
            val gz = buffer.readDouble()
            val pressure = buffer.readDouble()
            val stepFlag = buffer.readDouble() // 0.0 or 1.0

            val state =
                ActivityPipeline.feed(
                    timestamp = timestamp.toLong(),
                    ax = ax,
                    ay = ay,
                    az = az,
                    gx = gx,
                    gy = gy,
                    gz = gz,
                    pressure = pressure,
                    stepEvt = (stepFlag > 0.5),
                )
            _stateLiveData.postValue(state)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processActivityUpdate(data: ByteArray) {
        if (data.size < 4) return
        val type =
            java.nio.ByteBuffer
                .wrap(data)
                .int
        val mapped =
            when (type) {
                com.google.android.gms.location.DetectedActivity.WALKING -> ActivityState.WALKING
                com.google.android.gms.location.DetectedActivity.RUNNING -> ActivityState.RUNNING
                else -> ActivityState.EXERCISE // treat other ON_FOOT etc as exercise
            }
        ActivityPipeline.updateExternalDynamic(mapped)
    }

    // ByteArray에서 데이터 읽기 확장 함수
    private fun java.io.InputStream.readDouble(): Double {
        val bytes = ByteArray(8)
        read(bytes)
        return java.nio.ByteBuffer
            .wrap(bytes)
            .double
    }
}
