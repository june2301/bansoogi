package com.ddc.bansoogi.common.data.model

import com.ddc.bansoogi.calendar.data.model.toLocalDate
import com.ddc.bansoogi.common.data.domain.MealType
import com.ddc.bansoogi.common.data.local.TodayRecordDataSource
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TodayRecordModel {
    private val dataSource = TodayRecordDataSource()

    suspend fun initialize() {
        dataSource.initialize()
    }

    suspend fun updateIsClosed() {
        dataSource.updateIsClosed()
    }

    suspend fun renewTodayRecord() {
        dataSource.renewTodayRecord()
    }

    suspend fun interaction(recordId: ObjectId, addedEnergy: Int) {
        updateInteractionCnt(recordId)
        updateEnergy(recordId, addedEnergy)
        updateInteractionTime(recordId);
    }

    suspend fun updateInteractionCnt(recordId: ObjectId) {
        dataSource.updateInteractionCnt(recordId)
    }

    suspend fun updateEnergy(recordId: ObjectId, addedEnergy: Int) {
        dataSource.updateEnergy(recordId, addedEnergy)
    }

    suspend fun updateInteractionTime(recordId: ObjectId) {
        dataSource.updateInteractionTime(recordId)
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
                    interactionLatestTime = entity.interactionLatestTime,
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
                interactionLatestTime = entity.interactionLatestTime,
                isClosed = entity.isClosed,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }

    suspend fun markMealDone(recordId: ObjectId, mealType: MealType) {
        dataSource.markMealDone(recordId, mealType)
    }
}