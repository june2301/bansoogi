package com.example.eggi.main.controller

import com.example.eggi.common.data.local.TodayRecordDataSource
import com.example.eggi.common.data.model.TodayRecordModel
import com.example.eggi.main.view.TodayRecordView
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
            view.displayTodayRecord(todayRecord)
        }
    }

    fun initialize() {
        coroutineScope.launch {
            model.initialize()
            refreshTodayRecord()
        }
    }

    fun updateInteractionCnt(recordId: ObjectId) {
        coroutineScope.launch {
            model.updateInteractionCnt(recordId)
            refreshTodayRecord()
        }
    }

    fun updateEnergy(recordId: ObjectId, addedEnergy: Int) {
        coroutineScope.launch {
            model.updateEnergy(recordId, addedEnergy)
            refreshTodayRecord()
        }
    }
}