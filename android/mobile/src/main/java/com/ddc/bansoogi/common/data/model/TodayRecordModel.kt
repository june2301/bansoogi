package com.ddc.bansoogi.common.data.model

import com.ddc.bansoogi.common.data.local.TodayRecordDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId

class TodayRecordModel {
    private val dataSource = TodayRecordDataSource()

    suspend fun initialize() {
        dataSource.initialize()
    }

    suspend fun renewTodayRecord() {
        dataSource.renewTodayRecord()
    }

    suspend fun updateInteractionCnt(recordId: ObjectId) {
        dataSource.updateInteractionCnt(recordId)
    }

    suspend fun updateEnergy(recordId: ObjectId, addedEnergy: Int) {
        dataSource.updateEnergy(recordId, addedEnergy)
    }

    fun getTodayRecord(): Flow<TodayRecordDto?> {
        return dataSource.getTodayRecord().map { entity ->
            entity?.let {
                TodayRecordDto(
                    recordId = entity.recordId,
                    energyPoint = entity.energyPoint,
                    standUpCnt = entity.standUpCnt,
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

    fun getTodayRecordSync(): TodayRecordDto? {
        val entity = dataSource.getTodayRecordSync()
        return entity?.let {
            TodayRecordDto(
                recordId = entity.recordId,
                energyPoint = entity.energyPoint,
                standUpCnt = entity.standUpCnt,
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