package com.ddc.bansoogi.nearby.data

data class BansoogiFriend(
    val endpointId: String,
    val nickname: String,
    /** BLE 경로일 때만 채워짐 */
    val distanceRssi: Int? = null,
    val discoveredTime: Long = System.currentTimeMillis()
)