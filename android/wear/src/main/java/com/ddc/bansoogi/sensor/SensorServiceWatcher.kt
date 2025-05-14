package com.ddc.bansoogi.sensor

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import java.util.concurrent.TimeUnit

class SensorServiceWatcher(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() called – checking service status")
        if (!isServiceRunning(applicationContext, SensorForegroundService::class.java)) {
            Log.d(TAG, "Service not running – starting")
            SensorForegroundService.ensureRunning(applicationContext)
        } else {
            Log.d(TAG, "Service already running")
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
        private const val TAG = "SensorServiceWatcher"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<SensorServiceWatcher>(15, TimeUnit.MINUTES)
                .setInitialDelay(0, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    req
                )
            Log.d(TAG, "Periodic work scheduled")
        }

        fun runOnceNow(context: Context) {
            val req = OneTimeWorkRequestBuilder<SensorServiceWatcher>().build()
            WorkManager.getInstance(context).enqueue(req)
            Log.d(TAG, "One-time work enqueued")
        }
    }
}