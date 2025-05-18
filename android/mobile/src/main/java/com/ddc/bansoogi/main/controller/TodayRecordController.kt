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
import com.ddc.bansoogi.common.wear.data.mapper.WearDtoMapper
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
            model.updateEnergy(todayRecord.recordId, 10)
            model.markMealDone(todayRecord.recordId, mealType)

            val updated = model.getTodayRecordSync()
            updated?.let { dto ->
                val report = WearDtoMapper.toWearReport(dto)
                val energy = WearDtoMapper.toEnergy(dto)

                WearTodayRecordSender.send(context, report)
                WearTodayRecordSender.sendEnergy(context, energy)
            }
            refreshTodayRecord()
        }
    }
}