package com.ddc.bansoogi.main.ui.util

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class BansoogiState(val configValue: String) {
    BASIC("BASIC"),
    SMILE("SMILE"),
    PHONE("PHONE");

    companion object {
        fun fromString(value: String): BansoogiState {
            val cleanValue = value.trim('"')
            return BansoogiState.entries.find { it.configValue == cleanValue } ?: BASIC
        }
    }
}

object BansoogiStateHolder {
    var state by mutableStateOf(BansoogiState.BASIC)

    fun update(newState: BansoogiState) {
        state = newState
    }
}

data class BansoogiConfig (
    val sprite: String,
    val json: String
)

fun BansoogiState.getConfig(): BansoogiConfig {
    return when (this) {
        BansoogiState.BASIC -> BansoogiConfig("bansoogi_basic", "bansoogi_default_profile")
        BansoogiState.SMILE -> BansoogiConfig("bansoogi_smile_to_me", "bansoogi_smile_to_me")
        BansoogiState.PHONE -> BansoogiConfig("bansoogi_phone", "bansoogi_phone")
    }
}