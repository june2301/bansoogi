package com.example.eggi.main.controller

import com.example.eggi.common.data.local.TodayRecordDataSource
import com.example.eggi.common.data.model.TodayRecordModel
import com.example.eggi.main.view.TodayRecordView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainController(private val view: TodayRecordView) {
    private val todayRecordDataSource = TodayRecordDataSource()
    private val todayRecordModel = TodayRecordModel()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        scope.launch {
            todayRecordDataSource.initialize()
            todayRecordModel.getTodayRecord().collectLatest { info ->
                view.displayTodayRecord(info)
            }
        }
    }
}