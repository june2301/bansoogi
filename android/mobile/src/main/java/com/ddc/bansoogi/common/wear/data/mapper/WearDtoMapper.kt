package com.ddc.bansoogi.common.wear.data.mapper

import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.wear.data.model.WearReportDto

object WearDtoMapper {
    fun toReport(dto: TodayRecordDto): WearReportDto {
        return WearReportDto(
            energyPoint = dto.energyPoint,

            standupCount = dto.standUpCnt,
            stretchCount = dto.stretchCnt,
            phoneOffCount = dto.phoneOffCnt,

            lyingTime = dto.lyingTime,
            sittingTime = dto.sittingTime,
            phoneTime = dto.phoneTime,
            sleepTime = dto.sleepTime,

            breakfast = dto.breakfast,
            lunch = dto.lunch,
            dinner = dto.dinner,

            // 헬스 데이터 연동 필요
            walkCount = 0,
            runTime = 0,
            exerciseTime = 0,
            stairsClimbed = 0
        )
    }
}

