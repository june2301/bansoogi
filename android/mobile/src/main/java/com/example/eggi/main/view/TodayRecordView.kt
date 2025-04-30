package com.example.eggi.main.view

import com.example.eggi.common.data.model.TodayRecord
import com.example.eggi.myInfo.data.model.MyInfo

interface TodayRecordView {
    fun displayTodayRecord(todayRecord: TodayRecord)
}