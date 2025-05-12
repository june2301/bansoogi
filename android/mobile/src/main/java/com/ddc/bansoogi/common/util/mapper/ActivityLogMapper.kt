package com.ddc.bansoogi.common.util.mapper

object ActivityLogMapper {
    private val state = mapOf(
        "SITTING" to "앉아있음",
        "LYING" to "누워있음",
        "PHONE_IN_USE" to "핸드폰 사용"
    )

    fun String.toKoreanBehaviorState(): String {
        return state[this.uppercase()] ?: this
    }
}