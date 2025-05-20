package com.ddc.bansoogi.common.wear.data.mapper

import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.wear.communication.state.HealthStateHolder
import com.ddc.bansoogi.common.wear.data.model.WearMyInfoDto
import com.ddc.bansoogi.common.wear.data.model.WearReportDto
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto

object WearDtoMapper {
    fun toEnergy(dto: TodayRecordDto): Int {
        return dto.energyPoint
    }

    fun toWearReportWithHealthData(dto: TodayRecordDto): WearReportDto {
        var health = HealthStateHolder.healthData

        return WearReportDto(
            energyPoint = dto.energyPoint,

            standupCount = dto.standUpCnt,
            stretchCount = dto.stretchCnt,
            phoneOffCount = dto.phoneOffCnt,

            lyingTime = dto.lyingTime,
            sittingTime = dto.sittingTime,
            phoneTime = dto.phoneTime,

            walkCount = health?.step ?: 0,
            stairsClimbed = health?.floorsClimbed ?: 0.0f,
            sleepTime = health?.sleepData ?: 0,
            exerciseTime = health?.exerciseTime ?: 0,

            breakfast = dto.breakfast,
            lunch = dto.lunch,
            dinner = dto.dinner
        )
    }

    fun toWearMyInfo(dto: MyInfoDto): WearMyInfoDto {
        return WearMyInfoDto(
            wakeUpTime = dto.wakeUpTime,
            sleepTime = dto.sleepTime,

            breakfastTime = dto.breakfastTime,
            lunchTime = dto.lunchTime,
            dinnerTime = dto.dinnerTime,

            notificationDuration = dto.notificationDuration,

            notificationEnabled = dto.notificationEnabled,
            bgSoundEnabled = dto.bgSoundEnabled,
            effectSoundEnabled = dto.effectSoundEnabled
        )
    }
}

