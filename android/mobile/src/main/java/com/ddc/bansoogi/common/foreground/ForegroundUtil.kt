package com.ddc.bansoogi.common.foreground

import android.content.Context
import android.content.Intent

object ForegroundUtil {
    fun isServiceRunning(): Boolean {
        return ForegroundService.ServiceState.isRunning
    }

    fun startForegroundService(context: Context) {
        val intent = Intent(context, ForegroundService::class.java)
        context.startForegroundService(intent)
    }
}
