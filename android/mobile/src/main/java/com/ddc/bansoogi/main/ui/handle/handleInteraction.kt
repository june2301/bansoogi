package com.ddc.bansoogi.main.ui.handle

import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.main.ui.util.MAX_INTERACTION_COUNT
import com.ddc.bansoogi.main.ui.util.isInInteractionCooldown
import com.ddc.bansoogi.main.ui.util.isInSleepRange
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel
import kotlinx.coroutines.flow.firstOrNull

private const val ADD_INTERACTION_ENERGY = 5

suspend fun handleInteraction() {
    val myInfo = MyInfoModel().getMyInfoSync()
    val isInSleepRange = isInSleepRange(myInfo)

    TodayRecordModel().getTodayRecord().firstOrNull()?.let { dto ->
        handleInteraction(dto, isInSleepRange)
    }
}

suspend fun handleInteraction(todayRecordDto: TodayRecordDto, isInSleepRange: Boolean) {
    // 1. 현재 수면시간이 아니고, 현재 점수가 100 미만일때
    if (isInSleepRange || todayRecordDto.energyPoint >= 100) return

    // 2. 상호작용 최대 횟수 인지
    if (todayRecordDto.interactionCnt >= MAX_INTERACTION_COUNT) return

    // 3. 현대 쿨타임인지 확인
    if (isInInteractionCooldown(todayRecordDto.interactionLatestTime)) return

    TodayRecordModel().interaction(todayRecordDto.recordId, ADD_INTERACTION_ENERGY)
}
