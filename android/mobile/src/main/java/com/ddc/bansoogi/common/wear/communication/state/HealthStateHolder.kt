package com.ddc.bansoogi.common.wear.communication.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ddc.bansoogi.common.util.health.CustomHealthData

object HealthStateHolder {
    var healthData by mutableStateOf<CustomHealthData?>(null)

    fun update(newHealthData: CustomHealthData) {
        healthData = newHealthData
    }
}