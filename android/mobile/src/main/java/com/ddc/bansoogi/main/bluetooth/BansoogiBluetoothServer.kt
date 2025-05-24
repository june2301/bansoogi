package com.ddc.bansoogi.main.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log

class BansoogiBluetoothServer(private val context: Context) {

    companion object {
        private const val TAG = "BansoogiBluetoothServer"
    }

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null

    private var userNickname: String = ""
    private var isAdvertising = false

    @SuppressLint("MissingPermission")
    fun startServer(nickname: String) {
        userNickname = nickname

        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter?.isEnabled != true) {
            Log.w(TAG, "블루투스가 비활성화되어 있습니다")
            return
        }

        stopServer()

        setupGattServer()
        startAdvertising()
    }

    @SuppressLint("MissingPermission")
    private fun setupGattServer() {
        val gattServerCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                super.onConnectionStateChange(device, status, newState)
                Log.d(TAG, "연결 상태 변경: ${device?.address}, 상태: $newState")
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?
            ) {
                when (characteristic?.uuid) {
                    BansoogiBluetoothConstants.NICKNAME_CHARACTERISTIC_UUID -> {
                        val response = userNickname.toByteArray(Charsets.UTF_8)
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response)
                        Log.d(TAG, "닉네임 요청에 응답: $userNickname")
                    }
                    BansoogiBluetoothConstants.APP_IDENTIFIER_CHARACTERISTIC_UUID -> {
                        val response = BansoogiBluetoothConstants.BANSOOGI_APP_IDENTIFIER.toByteArray(Charsets.UTF_8)
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response)
                        Log.d(TAG, "앱 식별자 요청에 응답")
                    }
                    else -> {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    }
                }
            }
        }

        gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)

        // 서비스 생성
        val service = BluetoothGattService(
            BansoogiBluetoothConstants.BANSOOGI_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // 닉네임 특성 추가
        val nicknameCharacteristic = BluetoothGattCharacteristic(
            BansoogiBluetoothConstants.NICKNAME_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        // 앱 식별자 특성 추가
        val identifierCharacteristic = BluetoothGattCharacteristic(
            BansoogiBluetoothConstants.APP_IDENTIFIER_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        service.addCharacteristic(nicknameCharacteristic)
        service.addCharacteristic(identifierCharacteristic)

        gattServer?.addService(service)
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        if (isAdvertising) {
            Log.w(TAG, "이미 광고 중입니다")
            return
        }

        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(TAG, "BluetoothLeAdvertiser를 가져올 수 없습니다")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)    // 디바이스 이름 제외
            .setIncludeTxPowerLevel(false)  // TX 파워 제외
            // 16-bit Service UUID 사용 (기존 128-bit UUID 대신)
            .addServiceUuid(ParcelUuid.fromString("0000${BansoogiBluetoothConstants.BANSOOGI_SERVICE_UUID_16BIT.toString(16).uppercase().padStart(4, '0')}-0000-1000-8000-00805F9B34FB"))
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                isAdvertising = true
                Log.d(TAG, "BLE 광고 시작 성공")
            }

            override fun onStartFailure(errorCode: Int) {
                isAdvertising = false
                val errorMessage = when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> "광고 데이터가 너무 큽니다 (31바이트 초과)"
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "광고 인스턴스가 너무 많습니다"
                    ADVERTISE_FAILED_ALREADY_STARTED -> "이미 광고가 시작되었습니다"
                    ADVERTISE_FAILED_INTERNAL_ERROR -> "내부 오류가 발생했습니다"
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "기능이 지원되지 않습니다"
                    else -> "알 수 없는 오류: $errorCode"
                }
                Log.e(TAG, "BLE 광고 시작 실패: $errorMessage")
            }
        }

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopServer() {
        if (isAdvertising && advertiseCallback != null) {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            Log.d(TAG, "BLE 광고 중지")
        }

        gattServer?.clearServices()
        gattServer?.close()
        gattServer = null
        Log.d(TAG, "BLE 서버 정리 완료")
    }
}