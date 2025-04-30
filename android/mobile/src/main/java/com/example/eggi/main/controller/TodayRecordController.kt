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
    private val dataSource = TodayRecordDataSource()
    private val model = TodayRecordModel()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        coroutineScope.launch {
            dataSource.initialize()
            model.getTodayRecord().collectLatest { todayRecord ->
                view.displayTodayRecord(todayRecord)
            }
        }
    }

    fun updateInteractionCnt(recordId: ObjectId) {
        coroutineScope.launch {
            dataSource.updateInteractionCnt(recordId)
            model.getTodayRecord().collectLatest { todayRecord ->
                view.displayTodayRecord(todayRecord)
            }
        }
    }

    fun updateEnergy(recordId: ObjectId, addedEnergy: Int) {
        coroutineScope.launch {
            dataSource.updateEnergy(recordId, addedEnergy)
            model.getTodayRecord().collectLatest { todayRecord ->
                view.displayTodayRecord(todayRecord)
            }
        }
    }
}