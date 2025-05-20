package com.ddc.bansoogi.common.data.model

import android.util.Log
import com.ddc.bansoogi.calendar.data.model.toLocalDate
import com.ddc.bansoogi.calendar.ui.util.CalendarUtils
import com.ddc.bansoogi.common.data.enum.MealType
import com.ddc.bansoogi.common.data.local.TodayRecordDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId

class TodayRecordModel {
    private val dataSource = TodayRecordDataSource()
    private val logModel = ActivityLogModel()

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

    suspend fun updateAllEnergy(recordId: ObjectId, energy: Int) {
        dataSource.updateAllEnergy(recordId, energy)
    }

    suspend fun updateEnergy(recordId: ObjectId, addedEnergy: Int) {
        dataSource.updateEnergy(recordId, addedEnergy)
    }

    suspend fun updateInteractionTime(recordId: ObjectId) {
        dataSource.updateInteractionTime(recordId)
    }

    suspend fun updateIsViewed(recordId: ObjectId, viewed: Boolean) {
        dataSource.updateIsViewed(recordId, viewed)
    }

    fun isViewed(): Boolean {
        return dataSource.isViewed()
    }

    suspend fun updatePhoneOff(recordId: ObjectId) {
        // 핸드폰 off 횟수 증가
        dataSource.updatePhoneOffCnt(recordId)

        // 에너지 점수 증가
        dataSource.updateEnergy(recordId, 10)
    }

    suspend fun updatePhoneTime(time: Int) {
        val recordId = getTodayRecordSync()?.recordId

        if (recordId != null) {
            dataSource.updatePhoneTime(recordId, time)
        } else {
            Log.d("PhoneUsageEnergyUtil", "recordId가 null입니다. 핸드폰 사용 시간 추가 실패")
        }
    }

    fun getTodayRecord(): Flow<TodayRecordDto?> {
        return dataSource.getTodayRecord().map { entity ->
            entity?.let {
                // 로그 호출
                val localDate = entity.createdAt.toLocalDate()
                val date = CalendarUtils.toFormattedDateString(localDate, localDate.dayOfMonth)

                val standLog = logModel.getLogsByTypeAndDate("STANDUP", date)
                val stretchLog = logModel.getLogsByTypeAndDate("STRETCH", date)
                val phoneOffLog = logModel.getLogsByTypeAndDate("PHONE_OFF", date)

                TodayRecordDto(
                    recordId = entity.recordId,

                    energyPoint = entity.energyPoint,

                    standUpCnt = entity.standUpCnt,
                    standLog = standLog,

                    stretchCnt = entity.stretchCnt,
                    stretchLog = stretchLog,

                    phoneOffCnt = entity.phoneOffCnt,
                    phoneOffLog = phoneOffLog,

                    lyingTime = entity.lyingTime,
                    sittingTime = entity.sittingTime,
                    phoneTime = entity.phoneTime,
                    sleepTime = entity.sleepTime,

                    breakfast = entity.breakfast,
                    lunch = entity.lunch,
                    dinner = entity.dinner,

                    interactionCnt = entity.interactionCnt,
                    interactionLatestTime = entity.interactionLatestTime,

                    isViewed = entity.isViewed,
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
            // 로그 호출
            val localDate = entity.createdAt.toLocalDate()
            val date = CalendarUtils.toFormattedDateString(localDate, localDate.dayOfMonth)

            val standLog = logModel.getLogsByTypeAndDate("STANDUP", date)
            val stretchLog = logModel.getLogsByTypeAndDate("STRETCH", date)
            val phoneOffLog = logModel.getLogsByTypeAndDate("PHONE_OFF", date)

            TodayRecordDto(
                recordId = entity.recordId,

                energyPoint = entity.energyPoint,

                standUpCnt = entity.standUpCnt,
                standLog = standLog,

                stretchCnt = entity.stretchCnt,
                stretchLog = stretchLog,

                phoneOffCnt = entity.phoneOffCnt,
                phoneOffLog = phoneOffLog,

                lyingTime = entity.lyingTime,
                sittingTime = entity.sittingTime,
                phoneTime = entity.phoneTime,
                sleepTime = entity.sleepTime,

                breakfast = entity.breakfast,
                lunch = entity.lunch,
                dinner = entity.dinner,

                interactionCnt = entity.interactionCnt,
                interactionLatestTime = entity.interactionLatestTime,

                isViewed = entity.isViewed,
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