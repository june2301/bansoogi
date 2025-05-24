package com.ddc.bansoogi.common.wear.communication.sender

import android.content.Context
import com.ddc.bansoogi.common.wear.communication.CommunicationPaths
import com.ddc.bansoogi.main.ui.util.BansoogiState

object BansoogiStateSender {
    fun send(context: Context, state: BansoogiState) {
        MobileToWearMessageSender.sendData(
            context,
            state.name,
            CommunicationPaths.MobileToWear.BANSOOGI_ANIMATION
        )
    }
}