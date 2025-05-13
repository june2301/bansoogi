package com.ddc.bansoogi.main.controller

import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.main.ui.handle.handleInteraction
import com.ddc.bansoogi.main.view.TodayRecordView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId

class TodayRecordController(private val view: TodayRecordView) {
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
}