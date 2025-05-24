package com.ddc.bansoogi.main.bluetooth

import android.content.Context
import android.util.Log

class BansoogiFriendManager(private val context: Context) {

    companion object {
        private const val TAG = "BansoogiFriendManager"
    }

    private val bluetoothServer = BansoogiBluetoothServer(context)
    private val bluetoothClient = BansoogiBluetoothClient(context)

    val discoveredFriendsFlow = bluetoothClient.friendsFlow

    // 친구 발견 이벤트
    var onFriendFound: ((BansoogiFriend) -> Unit)? = null

    private var isServiceRunning = false

    init {
        bluetoothClient.onFriendDiscovered = { friend ->
            Log.d(TAG, "새로운 반숙이 친구: ${friend.nickname}")
            onFriendFound?.invoke(friend)
        }
    }

    // 반숙이 서비스 시작 (자신의 닉네임 광고 + 친구 찾기)
    fun startBansoogiService(userNickname: String) {
        if (isServiceRunning) {
            Log.w(TAG, "서비스가 이미 실행 중입니다")
            return
        }

        Log.d(TAG, "반숙이 친구 찾기 서비스 시작: $userNickname")

        // 서버 시작 (자신의 닉네임 광고)
        bluetoothServer.startServer(userNickname)

        // 잠시 대기 후 클라이언트 시작 (동시 시작 방지)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            bluetoothClient.startScanning()
        }, 2000)

        isServiceRunning = true
    }

    // 반숙이 서비스 중지
    fun stopBansoogiService() {
        if (!isServiceRunning) {
            Log.w(TAG, "서비스가 실행 중이 아닙니다")
            return
        }

        Log.d(TAG, "반숙이 친구 찾기 서비스 중지")
        bluetoothServer.stopServer()
        bluetoothClient.stopScanning()
        isServiceRunning = false
    }

    // 발견된 친구 목록 초기화
    fun clearFriends() {
        bluetoothClient.clearDiscoveredFriends()
    }
}