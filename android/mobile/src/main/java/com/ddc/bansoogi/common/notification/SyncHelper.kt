package com.ddc.bansoogi.common.notification

import android.content.Context
import com.ddc.bansoogi.common.wear.data.mapper.WearDtoMapper
import com.ddc.bansoogi.common.wear.communication.sender.WearMyInfoSender
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto

object SyncHelper {
    fun syncNotificationToWatch(context: Context, info: MyInfoDto) {
        val wearInfo = WearDtoMapper.toWearMyInfo(info)
        WearMyInfoSender.send(context, wearInfo)
    }
}
