package com.ddc.bansoogi.myInfo.data.model

data class MyInfoDto(
    val userId: String,
    val nickname: String,
    val birthDate: String,
    val profileBansoogiId: Int,
    val wakeUpTime: String,
    val sleepTime: String,
    val breakfastTime: String,
    val lunchTime: String,
    val dinnerTime: String,
    val notificationDuration: Int,
    val notificationEnabled: Boolean,
    val bgSoundEnabled: Boolean,
    val effectSoundEnabled: Boolean
)
