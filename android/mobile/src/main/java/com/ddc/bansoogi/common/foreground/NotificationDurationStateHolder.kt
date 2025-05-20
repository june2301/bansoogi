package com.ddc.bansoogi.common.foreground

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ddc.bansoogi.myInfo.data.model.MyInfoModel

object NotificationDurationStateHolder {
    var notificationDuration by mutableIntStateOf(0)

    fun update(newDuration: Int) {
        notificationDuration = newDuration
    }

    fun setDuration() {
        notificationDuration = MyInfoModel().getMyInfoSync()?.notificationDuration ?: 30
    }
}