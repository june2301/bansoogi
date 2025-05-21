package com.ddc.bansoogi.main.ui.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ddc.bansoogi.R
import com.ddc.bansoogi.common.mobile.communication.sender.BansoogiStateSender

enum class BansoogiState(val configValue: String) {
    BASIC("BASIC"),
    SMILE("SMILE"),
    PHONE("PHONE"),
    EAT("EAT"),
    LIE("LIE"),
    RUN("RUN"),
    SLEEP("SLEEP"),
    WALK("WALK");

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
        if (state != newState){
            state = newState
        }
    }

    fun updateWithMobile(context: Context, newState: BansoogiState) {
        if (state != newState) {
            update(newState)
        }

        BansoogiStateSender.send(context, newState)
    }

    fun background(): Int {
        if (state == BansoogiState.BASIC
            || state == BansoogiState.WALK
            || state == BansoogiState.RUN) {
            return R.drawable.background_sunny_sky
        }

        return R.drawable.background_kitchen
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
        BansoogiState.EAT -> BansoogiConfig("bansoogi_eat", "bansoogi_eat")
        BansoogiState.LIE -> BansoogiConfig("bansoogi_lie_breathe", "bansoogi_lie_breathe")
        BansoogiState.RUN -> BansoogiConfig("bansoogi_run", "bansoogi_run")
        BansoogiState.SLEEP -> BansoogiConfig("bansoogi_sleep_breathe", "bansoogi_sleep_breathe")
        BansoogiState.WALK -> BansoogiConfig("bansoogi_walk", "bansoogi_walk")
    }
}