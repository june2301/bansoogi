package com.ddc.bansoogi.sensor

import android.app.ActivityManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import java.util.concurrent.TimeUnit

class SensorServiceWatcher(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!isServiceRunning(applicationContext, SensorForegroundService::class.java)) {
            SensorForegroundService.ensureRunning(applicationContext)
        }
        return Result.success()
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val am = context.getSystemService(ActivityManager::class.java)
        @Suppress("DEPRECATION")
        val services = am.getRunningServices(Integer.MAX_VALUE)
        return services.any { it.service.className == serviceClass.name }
    }

    companion object {
        private const val UNIQUE_NAME = "sensor_service_watch"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<SensorServiceWatcher>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    req
                )
        }
    }
}
