package com.ddc.bansoogi.main.bluetooth

import java.util.UUID


object BansoogiBluetoothConstants {
    // 16-bit Service UUID로 변경 (공간 절약)
    const val BANSOOGI_SERVICE_UUID_16BIT: Short = 0x1ABC.toShort()

    // 16-bit를 128-bit UUID로 변환 (표준 Bluetooth UUID 형식)
    val BANSOOGI_SERVICE_UUID: UUID = UUID.fromString("0000${BANSOOGI_SERVICE_UUID_16BIT.toString(16).uppercase().padStart(4, '0')}-0000-1000-8000-00805F9B34FB")

    // 닉네임 읽기/쓰기 특성
    val NICKNAME_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789abd")

    // 앱 식별 특성
    val APP_IDENTIFIER_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789abe")

    // 반숙이 앱 식별자
    const val BANSOOGI_APP_IDENTIFIER = "BANSOOGI_V1"
}