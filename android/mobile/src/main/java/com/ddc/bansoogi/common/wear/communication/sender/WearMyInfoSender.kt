package com.ddc.bansoogi.common.wear.communication.sender

import android.content.Context
import com.ddc.bansoogi.common.wear.communication.CommunicationPaths
import com.ddc.bansoogi.common.wear.data.model.WearMyInfoDto

object WearMyInfoSender {
    fun send(context: Context, myInfoDto: WearMyInfoDto) {
        MobileToWearMessageSender.sendData(
            context,
            myInfoDto,
            CommunicationPaths.MobileToWear.MY_INFO_DATA
        )
    }
}