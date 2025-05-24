package com.ddc.bansoogi.main.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BansoogiBluetoothManager(private val context: Context) {

    companion object {
        private const val TAG = "BansoogiBluetoothManager"
    }

    private val friendManager = BansoogiFriendManager(context)
    private var bluetoothAdapter: BluetoothAdapter? = null

    // 상태들
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // 발견된 친구들 상태
    val discoveredFriends = friendManager.discoveredFriendsFlow

    // 콜백들
    var onFriendFound: ((String) -> Unit)? = null

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // 친구 발견 시 콜백 설정
        friendManager.onFriendFound = { friend ->
            onFriendFound?.invoke(friend.nickname)
        }
    }

    // 블루투스 지원 여부 확인
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    // 블루투스 활성화 여부 확인
    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    // 필요한 권한이 모두 있는지 확인
    fun hasAllPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE
            ).all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ).all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    // 반숙이 친구 찾기 시작
    fun triggerFriendSearch(): String {
        if (!isBluetoothSupported()) {
            return "이 기기는 블루투스를 지원하지 않아요"
        }

        if (!hasAllPermissions()) {
            return "블루투스 권한이 필요해요!"
        }

        if (!isBluetoothEnabled()) {
            return "블루투스를 켜주세요!"
        }

        // 사용자 닉네임 가져오기
        val userNickname = getUserNickname()

        _isScanning.value = true
        friendManager.startBansoogiService(userNickname)

        return "반숙이 친구들을 찾고 있어요!"
    }

    // 사용자 닉네임 가져오기
    private fun getUserNickname(): String {
        val prefs = context.getSharedPreferences("bansoogi_prefs", Context.MODE_PRIVATE)
        return prefs.getString("user_nickname", "반숙이") ?: "반숙이"
    }

    // 권한 승인 후 초기화
    fun initializeAfterPermissionGranted() {
        // 필요시 추가 초기화 로직
        Log.d(TAG, "권한 승인 후 초기화 완료")
    }

}