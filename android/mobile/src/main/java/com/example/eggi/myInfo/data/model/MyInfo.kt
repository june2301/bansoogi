package com.example.eggi.myInfo.data.model

data class MyInfo(
    val userId: String,
    val name: String,
    val birthDate: String,
    val profileBansoogiId: Int,
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
