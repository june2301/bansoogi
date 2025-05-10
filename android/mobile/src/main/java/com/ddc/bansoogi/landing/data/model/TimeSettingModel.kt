package com.ddc.bansoogi.landing.data.model

class TimeSettingModel {
    var wakeUpTime: String = ""
    var bedTimeTime: String = ""
    var breakfastTime: String = ""
    var lunchTime: String = ""
    var dinnerTime: String = ""
    var durationMinutes: Int = 15

    fun reset() {
        wakeUpTime = ""
        bedTimeTime = ""
        breakfastTime = ""
        lunchTime = ""
        dinnerTime = ""
        durationMinutes = 15
    }
}
