package com.example.eggi.common.data.model

import com.example.eggi.common.data.model.TodayRecord
import com.example.eggi.common.data.local.TodayRecordDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TodayRecordModel {
    private val dataSource = TodayRecordDataSource()

    fun getTodayRecord(): Flow<TodayRecord> {
        return dataSource.getTodayRecord().map { entity ->
                TodayRecord(
                    recordId = entity.recordId.toString(),
                    energyPoint = entity.energyPoint,
                    stretchCnt = entity.stretchCnt,
                    phoneOffCnt = entity.phoneOffCnt,
                    lyingTime = entity.lyingTime,
                    sittingTime = entity.sittingTime,
                    phoneTime = entity.phoneTime,
                    sleepTime = entity.sleepTime,
                    breakfast = entity.breakfast,
                    lunch = entity.lunch,
                    dinner = entity.dinner,
                    interactionCnt = entity.interactionCnt,
                    isClosed = entity.isClosed,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
        }
    }
}