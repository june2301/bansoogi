package com.ddc.bansoogi.myinfo.data

data class MyInfoDto (
    val wakeUpTime: String,
    val sleepTime: String,
    val breakfastTime: String,
    val lunchTime: String,
    val dinnerTime: String,
    val notificationDuration: Int,
    val alarmEnabled: Boolean,
    val bgSoundEnabled: Boolean,
    val effectSoundEnabled: Boolean
)