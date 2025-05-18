package com.ddc.bansoogi.main.controller

import com.ddc.bansoogi.common.data.enum.MealType
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.common.wear.communication.sender.WearTodayRecordSender
import com.ddc.bansoogi.main.ui.handle.handleInteraction
import com.ddc.bansoogi.main.view.TodayRecordView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Context
import com.ddc.bansoogi.common.wear.communication.receiver.RequestHandler
import com.ddc.bansoogi.common.util.health.CustomHealthData
import com.ddc.bansoogi.common.util.health.EnergyUtil
import com.ddc.bansoogi.common.wear.data.mapper.WearDtoMapper
import com.ddc.bansoogi.main.data.model.TodayHealthDataDto
import com.ddc.bansoogi.calendar.ui.util.CalendarUtils
import java.time.LocalDate
import org.mongodb.kbson.ObjectId

class TodayRecordController(
    private val view: TodayRecordView,
    private val context: Context
) {
    private val model = TodayRecordModel()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private suspend fun refreshTodayRecord() {
        model.getTodayRecord().collectLatest { todayRecord ->
            todayRecord?.let {
                view.displayTodayRecord(todayRecord)
            } ?: run {
                view.showEmptyState()
            }
        }
    }

    fun initialize() {
        coroutineScope.launch {
            model.initialize()
            refreshTodayRecord()
        }
    }

    fun updateIsClosed() {
        coroutineScope.launch {
            model.updateIsClosed()
            refreshTodayRecord()
        }
    }

    fun renewTodayRecord() {
        coroutineScope.launch {
            model.renewTodayRecord()
            refreshTodayRecord()
        }
    }

    fun onInteract(todayRecord: TodayRecordDto, isInSleepRange: Boolean) {
        coroutineScope.launch {
            handleInteraction(todayRecord, isInSleepRange)
        }

        RequestHandler(context, coroutineScope).handleTodayRecordRequest()
    }

    fun getTodayRecordSync(): TodayRecordDto? {
        return model.getTodayRecordSync()
    }

    fun isViewed(): Boolean {
        return model.isViewed()
    }

    fun updateIsViewed(recordId: ObjectId, viewed: Boolean) {
        coroutineScope.launch {
            model.updateIsViewed(recordId, viewed)
            refreshTodayRecord()
        }
    }

    fun checkMeal(todayRecord: TodayRecordDto, mealType: MealType) {
        coroutineScope.launch {
            model.markMealDone(todayRecord.recordId, mealType)

            val dateStr = CalendarUtils.toFormattedDateString(
                LocalDate.now(), LocalDate.now().dayOfMonth
            )
            val healthDto: TodayHealthDataDto? =
                TodayHealthDataController().getTodayHealthData(dateStr)
            val healthData = CustomHealthData(
                step           = healthDto?.steps?.toLong() ?: 0L,
                stepGoal       = healthDto?.stepGoal          ?: 0,
                floorsClimbed  = healthDto?.floorsClimbed?.toFloat() ?: 0f,
                sleepData      = healthDto?.sleepTime,
                exerciseTime   = healthDto?.exerciseTime
            )

            val newEnergy = EnergyUtil.calculateEnergyOnce(healthData)
            model.updateAllEnergy(todayRecord.recordId, newEnergy)

            model.getTodayRecordSync()?.let { dto ->
                WearTodayRecordSender.send(context, WearDtoMapper.toWearReport(dto))
                WearTodayRecordSender.sendEnergy(context, WearDtoMapper.toEnergy(dto))
            }
            refreshTodayRecord()
        }
    }
}