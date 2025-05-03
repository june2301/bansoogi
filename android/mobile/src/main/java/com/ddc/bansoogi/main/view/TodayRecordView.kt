package com.ddc.bansoogi.main.view

import com.ddc.bansoogi.common.data.model.TodayRecord

interface TodayRecordView {
    fun displayTodayRecord(todayRecord: TodayRecord)
    fun showEmptyState()
}