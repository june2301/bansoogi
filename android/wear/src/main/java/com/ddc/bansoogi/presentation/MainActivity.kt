package com.ddc.bansoogi.presentation

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
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

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                showCustomToast()
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


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        requestNotificationPermissionOnce()
        NotificationHelper.registerChannels(this)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            BansoogiApp()
        }
    }

    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED_NOTI, false)) return
        prefs.edit().putBoolean(KEY_ASKED_NOTI, true).apply()

        // POST_NOTIFICATIONS 권한 요청
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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