package com.ddc.bansoogi.main.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission

data class BansoogiFriend(
    val device: BluetoothDevice,
    val nickname: String,
    val rssi: Int,
    val discoveredTime: Long = System.currentTimeMillis()
) {
    val deviceAddress: String get() = device.address
    val deviceName: String @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    get() = device.name ?: "Unknown Device"
}