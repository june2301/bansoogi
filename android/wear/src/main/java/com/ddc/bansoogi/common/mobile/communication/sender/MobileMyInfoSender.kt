package com.ddc.bansoogi.common.mobile.communication.sender

import android.content.Context
import com.ddc.bansoogi.common.mobile.communication.CommunicationPaths

object MobileMyInfoSender {
    fun send(context: Context) {
        WearToMobileMessageSender.sendReqeust(
            context,
            CommunicationPaths.WearToMobile.MT_INFO_REQUEST
        )
    }
}