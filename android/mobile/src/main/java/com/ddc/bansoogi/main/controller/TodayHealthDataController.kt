package com.ddc.bansoogi.main.controller

import com.ddc.bansoogi.calendar.data.model.RecordedReportModel
import com.ddc.bansoogi.collection.data.model.CollectionModel
import com.ddc.bansoogi.main.data.model.TodayHealthDataDto
import com.ddc.bansoogi.main.data.model.TodayHealthDataModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.JdkConstants

class TodayHealthDataController() {
    private val model = TodayHealthDataModel()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun initialize(date: String) {
        coroutineScope.launch {
            model.initialize(date)

            CollectionModel().initialize()
            RecordedReportModel().initialize()
        }
    }

    fun getTodayHealthData(date: String): TodayHealthDataDto? {
        return model.getTodayHealthData(date)
    }

    fun updateTodayHealthData(date: String, stepGoal: Int?, steps: Int?, floorsClimbed: Int?, sleepTime: Int?, exerciseTime: Int?) {
        coroutineScope.launch {
            model.updateTodayHealthData(date, stepGoal, steps, floorsClimbed, sleepTime, exerciseTime)
        }
    }
}