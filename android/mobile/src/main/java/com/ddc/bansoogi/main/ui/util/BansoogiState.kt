package com.ddc.bansoogi.main.ui.util

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ddc.bansoogi.common.wear.communication.sender.BansoogiStateSender

enum class BansoogiState(val configValue: String) {
    BASIC("BASIC"),
    SMILE("SMILE"),
    PHONE("PHONE");
}

object BansoogiStateHolder {
    var state by mutableStateOf(BansoogiState.BASIC)

    fun update(context: Context, newState: BansoogiState) {
        if (state != newState) {
            state = newState
            BansoogiStateSender.send(context, state)
        }
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