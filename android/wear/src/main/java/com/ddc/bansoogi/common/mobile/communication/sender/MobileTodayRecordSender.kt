package com.ddc.bansoogi.common.mobile.communication.sender

import android.content.Context
import com.ddc.bansoogi.common.mobile.communication.CommunicationPaths

object MobileTodayRecordSender {
    fun sendEnergyRequest(context: Context) {
        WearToMobileMessageSender.sendReqeust(
            context,
            CommunicationPaths.WearToMobile.ENERGY_REQUEST
        )
    }

    fun send(context: Context) {
        WearToMobileMessageSender.sendReqeust(
            context,
            CommunicationPaths.WearToMobile.TODAY_RECORD_REQUEST
        )
    }
}