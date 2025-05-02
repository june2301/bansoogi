package com.example.eggi.main.controller

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
            todayRecord?.let {
                view.displayTodayRecord(todayRecord)
            } ?: run {
                view.showEmptyState()
            }
        }
    }

    fun initialize() {
        coroutineScope.launch {
            // TODO: 알 깨기, 결산에 따라 객체가 생성되므로 추후 수정이 필요함.
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