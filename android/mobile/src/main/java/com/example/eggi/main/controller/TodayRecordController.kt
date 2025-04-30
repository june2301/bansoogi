package com.example.eggi.main.controller

import com.example.eggi.common.data.model.TodayRecordModel
import com.example.eggi.main.view.TodayRecordView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class TodayRecordController(private val view: TodayRecordView) {
    private val model = TodayRecordModel()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        loadTotalRecord()
    }

    private fun loadTotalRecord() {
        coroutineScope.launch {
            model.getTodayRecord().collectLatest { totalRecord ->
                view.displayTodayRecord(totalRecord)
            }
        }
    }
}