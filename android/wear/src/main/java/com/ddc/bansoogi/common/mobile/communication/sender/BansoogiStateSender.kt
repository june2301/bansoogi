package com.ddc.bansoogi.common.mobile.communication.sender

import android.content.Context
import com.ddc.bansoogi.common.mobile.communication.CommunicationPaths
import com.ddc.bansoogi.main.ui.util.BansoogiState

object BansoogiStateSender {
    fun send(context: Context, state: BansoogiState) {
        WearToMobileMessageSender.sendData(
            context,
            state.name,
            CommunicationPaths.WearToMobile.BANSOOGI_ANIMATION
        )
    }
}