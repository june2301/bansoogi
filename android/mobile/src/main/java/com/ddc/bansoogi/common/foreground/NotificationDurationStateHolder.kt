package com.ddc.bansoogi.common.foreground

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object NotificationDurationStateHolder {
    var notificationDuration by mutableIntStateOf(30)

    fun update(newDuration: Int) {
        notificationDuration = newDuration
    }
}