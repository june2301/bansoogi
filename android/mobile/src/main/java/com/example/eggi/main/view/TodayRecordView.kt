package com.example.eggi.main.view

import com.example.eggi.common.data.model.TodayRecord

interface TodayRecordView {
    fun displayTodayRecord(todayRecord: TodayRecord)
    fun showEmptyState()
}