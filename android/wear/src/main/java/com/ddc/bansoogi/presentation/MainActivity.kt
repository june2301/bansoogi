package com.ddc.bansoogi.presentation

import android.content.Intent
import android.Manifest
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.wear.tooling.preview.devices.WearDevices
import com.ddc.bansoogi.common.navigation.WearNavGraph
import com.ddc.bansoogi.common.notification.NotificationHelper
import com.ddc.bansoogi.sensor.SensorForegroundService
import com.ddc.bansoogi.sensor.SensorServiceWatcher

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.all { it.value }
            if (!allGranted) {
                showCustomToast()
            } else {
                maybeRequestIgnoreBatteryOptimization()
            }
        }

    private fun showCustomToast() {
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(Color.WHITE)
        }

        val textView = TextView(this).apply {
            text = "앱 설정에서 알림 권한을 허용하시면\n워치 전용 알림을 받을 수 있습니다."
            background = backgroundDrawable
            setTextColor(Color.BLACK)
            textSize = 12f
            setPadding(32, 24, 32, 24)
            gravity = Gravity.CENTER
        }

        Toast(this).apply {
            duration = Toast.LENGTH_LONG
            view = textView
            show()
        }
    }

    private fun maybeRequestIgnoreBatteryOptimization() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            SensorForegroundService.ensureRunning(this)
        } else {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:$packageName"))
            startActivity(intent)
            SensorForegroundService.ensureRunning(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        requestNotificationPermissionOnce()
        NotificationHelper.registerChannels(this)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            BansoogiApp()
        }
        SensorServiceWatcher.schedule(this)
        SensorServiceWatcher.runOnceNow(this) // Temporary debug call
    }

    private fun requestNotificationPermissionOnce() {
        val needNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        val requiredPermissions = mutableListOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION
        ).apply {
            if (needNotificationPermission) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= 34) add("android.permission.FOREGROUND_SERVICE_HEALTH")
        }.toTypedArray()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ASKED_NOTI, false)) {
            prefs.edit().putBoolean(KEY_ASKED_NOTI, true).apply()
            requestPermissionLauncher.launch(requiredPermissions)
        } else {
            val allGranted = requiredPermissions.all { checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                maybeRequestIgnoreBatteryOptimization()
            }
        }
    }

    companion object {
        private const val KEY_ASKED_NOTI = "asked_notification_permission"
    }
}

@Composable
fun BansoogiApp() {
    val navController = rememberNavController()
    WearNavGraph(navController = navController)
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    BansoogiApp()
}