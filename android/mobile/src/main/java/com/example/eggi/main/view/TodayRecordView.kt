package com.example.eggi.main.view

import com.example.eggi.common.data.model.TodayRecordDto

interface TodayRecordView {
    fun displayTodayRecord(todayRecordDto: TodayRecordDto)
    fun showEmptyState()
}