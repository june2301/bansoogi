package com.ddc.bansoogi.main.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BansoogiBluetoothClient(private val context: Context) {

    companion object {
        private const val TAG = "BansoogiBluetoothClient"
    }

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    private val discoveredFriends = mutableMapOf<String, BansoogiFriend>()
    private val _friendsFlow = MutableStateFlow<List<BansoogiFriend>>(emptyList())
    val friendsFlow: StateFlow<List<BansoogiFriend>> = _friendsFlow.asStateFlow()

    // 친구 발견 콜백
    var onFriendDiscovered: ((BansoogiFriend) -> Unit)? = null

    private var isScanning = false

    init {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) {
            Log.w(TAG, "스캔이 실행 중이 아닙니다")
            return
        }

        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "반숙이 친구 스캔 중지")
        } catch (e: Exception) {
            Log.e(TAG, "스캔 중지 중 예외 발생: ${e.message}")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi

            // 이미 발견된 기기인지 확인
            if (discoveredFriends.containsKey(device.address)) {
                return
            }

            Log.d(TAG, "반숙이 앱을 가진 기기 발견: ${device.address}")

            // GATT 연결하여 닉네임 가져오기
            connectAndGetNickname(device, rssi)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            val errorMessage = when (errorCode) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "이미 스캔이 시작되었습니다"
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "앱 등록 실패"
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "내부 오류"
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "기능이 지원되지 않습니다"
                else -> "알 수 없는 오류: $errorCode"
            }
            Log.e(TAG, "스캔 실패: $errorMessage")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectAndGetNickname(device: BluetoothDevice, rssi: Int) {
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "GATT 연결 성공: ${device.address}")
                    gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "GATT 연결 해제: ${device.address}")
                    gatt?.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt?.getService(BansoogiBluetoothConstants.BANSOOGI_SERVICE_UUID)

                    // 먼저 앱 식별자 확인
                    val identifierCharacteristic = service?.getCharacteristic(
                        BansoogiBluetoothConstants.APP_IDENTIFIER_CHARACTERISTIC_UUID
                    )

                    identifierCharacteristic?.let {
                        gatt.readCharacteristic(it)
                    }
                } else {
                    Log.e(TAG, "서비스 발견 실패: $status")
                    gatt?.disconnect()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                    handleCharacteristicRead(gatt, characteristic, characteristic.value)
                }
            }

            // Android 13+ 호환성을 위한 새로운 콜백
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    handleCharacteristicRead(gatt, characteristic, value)
                } else {
                    Log.e(TAG, "특성 읽기 실패: ${device.address}, status: $status")
                    gatt.disconnect()
                }
            }

            private fun handleCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                val data = String(value, Charsets.UTF_8)

                when (characteristic.uuid) {
                    BansoogiBluetoothConstants.APP_IDENTIFIER_CHARACTERISTIC_UUID -> {
                        if (data == BansoogiBluetoothConstants.BANSOOGI_APP_IDENTIFIER) {
                            // 반숙이 앱 확인됨, 이제 닉네임 읽기
                            val service = gatt?.getService(BansoogiBluetoothConstants.BANSOOGI_SERVICE_UUID)
                            val nicknameCharacteristic = service?.getCharacteristic(
                                BansoogiBluetoothConstants.NICKNAME_CHARACTERISTIC_UUID
                            )
                            if (nicknameCharacteristic != null) {
                                val readResult = gatt.readCharacteristic(nicknameCharacteristic)
                                Log.d(TAG, "닉네임 읽기 요청 결과: $readResult")
                            } else {
                                Log.e(TAG, "닉네임 특성을 찾을 수 없음: ${device.address}")
                                gatt?.disconnect()
                            }
                        } else {
                            Log.d(TAG, "반숙이 앱이 아닌 기기: ${device.address}")
                            gatt?.disconnect()
                        }
                    }
                    BansoogiBluetoothConstants.NICKNAME_CHARACTERISTIC_UUID -> {
                        val nickname = data
                        Log.d(TAG, "반숙이 친구 발견: $nickname (${device.address})")

                        val friend = BansoogiFriend(device, nickname, rssi)
                        discoveredFriends[device.address] = friend
                        updateFriendsFlow()

                        // 콜백 호출
                        onFriendDiscovered?.invoke(friend)

                        gatt?.disconnect()
                    }
                }
            }
        }

        try {
            device.connectGatt(context, false, gattCallback)
        } catch (e: Exception) {
            Log.e(TAG, "GATT 연결 중 예외 발생: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (bluetoothAdapter?.isEnabled != true) {
            Log.w(TAG, "블루투스가 비활성화되어 있습니다")
            return
        }

        if (isScanning) {
            Log.w(TAG, "이미 스캔 중입니다")
            return
        }

        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner를 가져올 수 없습니다")
            return
        }

        // 16-bit Service UUID로 필터링
        val scanFilter = ScanFilter.Builder()
            // 16-bit Service UUID로 필터링 (기존 128-bit UUID 대신)
            .setServiceUuid(ParcelUuid.fromString("0000${BansoogiBluetoothConstants.BANSOOGI_SERVICE_UUID_16BIT.toString(16).uppercase().padStart(4, '0')}-0000-1000-8000-00805F9B34FB"))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .build()

        try {
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            isScanning = true
            Log.d(TAG, "반숙이 친구 스캔 시작")
        } catch (e: Exception) {
            Log.e(TAG, "스캔 시작 중 예외 발생: ${e.message}")
            isScanning = false
        }
    }

    private fun updateFriendsFlow() {
        _friendsFlow.value = discoveredFriends.values.toList()
    }

    fun clearDiscoveredFriends() {
        discoveredFriends.clear()
        updateFriendsFlow()
    }

    fun getDiscoveredFriends(): List<BansoogiFriend> = discoveredFriends.values.toList()
}