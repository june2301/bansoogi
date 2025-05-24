package com.ddc.bansoogi.common.wear.data.model

data class WearMyInfoDto (
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