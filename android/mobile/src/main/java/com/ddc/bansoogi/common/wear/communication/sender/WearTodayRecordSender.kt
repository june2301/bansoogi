package com.ddc.bansoogi.common.wear.communication.sender

import android.content.Context
import com.ddc.bansoogi.common.wear.communication.CommunicationPaths
import com.ddc.bansoogi.common.wear.data.model.WearReportDto

object WearTodayRecordSender {
    fun send(context: Context, reportDto: WearReportDto) {
        MobileToWearMessageSender.sendData(
            context,
            reportDto,
            CommunicationPaths.MobileToWear.TODAY_RECORD_DATA
        )
    }
}