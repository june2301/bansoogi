package com.example.eggi.main.controller

import com.example.eggi.common.data.local.TodayRecordDataSource
import com.example.eggi.common.data.model.TodayRecordModel
import com.example.eggi.main.view.TodayRecordView
import com.example.eggi.myInfo.data.local.MyInfoDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TodayRecordController(private val view: TodayRecordView) {
    private val dataSource = TodayRecordDataSource()
    private val model = TodayRecordModel()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        coroutineScope.launch {
            dataSource.initialize()
            model.getTodayRecord().collectLatest { totalRecord ->
                view.displayTodayRecord(totalRecord)
            }
        }
    }
}