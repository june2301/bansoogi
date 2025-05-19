package com.ddc.bansoogi.main.ui.handle

import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.util.health.EnergyManager
import com.ddc.bansoogi.main.ui.util.InteractionUtil
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import kotlinx.coroutines.flow.firstOrNull

suspend fun handleInteraction() {
    val myInfo = MyInfoModel().getMyInfoSync()
    val isInSleepRange = InteractionUtil.isInSleepRange(myInfo)

    TodayRecordModel().getTodayRecord().firstOrNull()?.let { dto ->
        handleInteraction(dto, isInSleepRange)
    }
}

suspend fun handleInteraction(todayRecord: TodayRecordDto, isInSleepRange: Boolean) {
    if (isInSleepRange
        || !InteractionUtil.isInteractionConditionMet(todayRecord)
        || InteractionUtil.isInInteractionCooldown(todayRecord.interactionLatestTime)) return

    TodayRecordModel().updateInteractionCnt(todayRecord.recordId)
    TodayRecordModel().updateInteractionTime(todayRecord.recordId)

    EnergyManager().energyRecalcAndSave()
}
