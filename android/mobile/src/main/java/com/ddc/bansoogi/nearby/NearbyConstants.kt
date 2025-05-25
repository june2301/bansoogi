package com.ddc.bansoogi.nearby

object NearbyConstants {
    /** 모든 기기가 공유할 Service ID (패키지명 사용 권장) */
    const val SERVICE_ID = "com.ddc.bansoogi.SERVICE"

    /** Payload types (추가 타입 생기면 확장) */
    const val TYPE_NICK        : Byte = 0x01
    const val TYPE_STATIC_WARN : Byte = 0x02       // ★ 추가
    const val TYPE_ALERT       : Byte = 0x03
    const val TYPE_STATE       : Byte = 0x04
}
