package com.ddc.bansoogi.common.wear.communication.receiver

import android.content.Context
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder
import kotlinx.coroutines.CoroutineScope

class StateHandler(
    private val context: Context,
    private val scope: CoroutineScope
) {
    fun handleBansoogiStateData(data: ByteArray) {
        val state = BansoogiState.fromString(String(data))

        BansoogiStateHolder.update(state)
    }
}