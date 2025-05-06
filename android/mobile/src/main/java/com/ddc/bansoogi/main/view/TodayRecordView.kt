package com.ddc.bansoogi.main.view

import com.ddc.bansoogi.common.data.model.TodayRecordDto

interface TodayRecordView {
    fun displayTodayRecord(todayRecord: TodayRecordDto)
    fun showEmptyState()
}