package com.ddc.bansoogi.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import androidx.wear.tooling.preview.devices.WearDevices
import com.ddc.bansoogi.common.navigation.WearNavGraph
import com.ddc.bansoogi.common.notification.NotificationHelper
import com.ddc.bansoogi.sensor.SensorForegroundService
import com.ddc.bansoogi.sensor.SensorServiceWatcher

class MainActivity : ComponentActivity() {

    // 1) 런타임 권한 요청 런처
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            startSensorsIfReady()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)
        setContent { BansoogiApp() }

        // 알림 채널은 바로 등록
        NotificationHelper.registerChannels(this)

        // 권한 체크 → 서비스 기동
        requestRuntimePermissions()
    }

    override fun onResume() {
        super.onResume()
        // 권한 변경 후에도 한 번 더 기동 시도
        startSensorsIfReady()
    }

    // 2) 필요한 런타임 권한 목록
    private val requiredPermissions: Array<String> by lazy {
        buildList {
            add(Manifest.permission.BODY_SENSORS)
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            add(Manifest.permission.HIGH_SAMPLING_RATE_SENSORS)
            if (Build.VERSION.SDK_INT >= 34) {
                add(Manifest.permission.FOREGROUND_SERVICE_HEALTH)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    // 3) 권한이 없으면 요청, 이미 있으면 바로 서비스 시작
    private fun requestRuntimePermissions() {
        val denied = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (denied.isNotEmpty()) {
            permissionLauncher.launch(denied.toTypedArray())
        } else {
            startSensorsIfReady()
        }
    }

    // 4) 권한만 충족되면 포그라운드 서비스 & 워커 스케줄
    private fun startSensorsIfReady() {
        val permissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        Log.d("MainActivity", "permissionsGranted=$permissionsGranted")

        if (permissionsGranted) {
            Log.d("MainActivity", ">> PERMISSIONS OK: starting sensors")
            SensorForegroundService.ensureRunning(this)
            SensorServiceWatcher.schedule(this)
        } else {
            Log.d("MainActivity", ">> permissions MISSING: showing rationale")
            showRationaleToast()
        }
    }

    // 5) 권한 거부 시 안내
    private fun showRationaleToast() {
        val tv = TextView(this).apply {
            text = "앱 권한이 부족하여 센서 기능이 제한됩니다.\n" +
                    "앱 설정에서 권한을 모두 허용해주세요."
            setPadding(32, 24, 32, 24)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(0xFFFFFFFF.toInt())
            }
        }
        Toast(this).apply {
            duration = Toast.LENGTH_LONG
            view = tv
            show()
        }
    }
}

@Composable
fun BansoogiApp() {
    val navController = rememberNavController()
    WearNavGraph(navController)
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun PreviewBansoogi() {
    BansoogiApp()
}
