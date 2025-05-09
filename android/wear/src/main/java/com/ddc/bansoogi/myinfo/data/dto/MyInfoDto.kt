package com.ddc.bansoogi.myinfo.data.dto

import kotlin.String

data class MyInfoDto (
    val wakeUpTime: String,
    val sleepTime: String,

    val breakfastTime: String,
    val lunchTime: String,
    val dinnerTime: String,

    val notificationDuration: Int,

    val notificationEnabled: Boolean,
    val bgSoundEnabled: Boolean,
    val effectSoundEnabled: Boolean
) {
    companion object {
        fun default() = MyInfoDto(
            wakeUpTime = "00:00",
            sleepTime = "00:00",

            breakfastTime = "00:00",
            lunchTime = "00:00",
            dinnerTime = "00:00",

            notificationDuration = 0,

            notificationEnabled = false,
            bgSoundEnabled = false,
            effectSoundEnabled = false
        )
    }
}