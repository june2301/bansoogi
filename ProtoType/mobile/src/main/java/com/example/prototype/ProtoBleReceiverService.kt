package com.example.prototype

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class ProtoBleReceiverService :
    Service(),
    MessageClient.OnMessageReceivedListener {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "proto_ble_channel"
        private const val TAG = "ProtoBleReceiverService"

        // DataLayer path for activity updates
        private const val ACTIVITY_UPDATE_PATH = "/activity_update"
        private const val WALK_STATE_PATH = "/walk_state"
        private const val POSE_STATE_PATH = "/pose_state"

        // LiveData for activity state (0:idle,1:walk,2:run,3:ascend)
        private val _activityLiveData = MutableLiveData<Int>()
        val activityLiveData: LiveData<Int> = _activityLiveData

        // LiveData for static pose (0:sitting,1:lying,2:standing)
        private val _poseLiveData = MutableLiveData<Int>()
        val poseLiveData: LiveData<Int> = _poseLiveData
    }

    private lateinit var messageClient: MessageClient

    override fun onCreate() {
        super.onCreate()

        // 알림 채널 생성
        createNotificationChannel()

        // 포그라운드 서비스 시작
        startForeground(NOTIFICATION_ID, createNotification())

        // Wearable API 클라이언트 초기화 (MessageClient only)
        messageClient = Wearable.getMessageClient(this)

        // 리스너 등록
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

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            ACTIVITY_UPDATE_PATH, WALK_STATE_PATH -> {
                Log.d(TAG, "RX activity path=${messageEvent.path} len=${messageEvent.data.size} byte0=${messageEvent.data.getOrNull(0)}")
                processActivityState(messageEvent.data)
            }
            POSE_STATE_PATH -> {
                Log.d(TAG, "RX pose path=${messageEvent.path} len=${messageEvent.data.size} byte0=${messageEvent.data.getOrNull(0)}")
                processPoseState(messageEvent.data)
            }
            else -> Log.d(TAG, "Unhandled path ${messageEvent.path}")
        }
    }

    private fun processActivityState(data: ByteArray) {
        if (data.isEmpty()) return
        val state = data[0].toInt()
        _activityLiveData.postValue(state)
    }

    private fun processPoseState(data: ByteArray) {
        if (data.isEmpty()) return
        val pose = data[0].toInt()
        _poseLiveData.postValue(pose)
    }
}
