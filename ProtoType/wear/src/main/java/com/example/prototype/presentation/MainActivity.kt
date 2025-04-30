// File: com/example/prototype/presentation/MainActivity.kt
package com.example.prototype.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.prototype.ProtoWearSensorService
import com.example.prototype.presentation.theme.protoTypeTheme
import kotlinx.coroutines.delay
import androidx.wear.compose.material.MaterialTheme as WearMaterialTheme

class MainActivity : ComponentActivity() {
    private val requestActivityPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startSensorService()
            } else {
                // 권한 거부 시 토스트나 안내 UI를 띄워줄 수 있습니다.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 1) Android Q 이상에서는 ACTIVITY_RECOGNITION 런타임 권한이 필요합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                )
            ) {
                PackageManager.PERMISSION_GRANTED -> {
                    startSensorService()
                }
                else -> {
                    requestActivityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            // Q 미만에서는 런타임 권한이 없으므로 바로 시작
            startSensorService()
        }

        // 2) UI 설정
        setContent {
            postureMonitorApp()
        }
    }

    private fun startSensorService() {
        val serviceIntent = Intent(this, ProtoWearSensorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}

@Composable
fun postureMonitorApp() {
    protoTypeTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            TimeText()
            sensorStatusScreen()
        }
    }
}

@Composable
fun sensorStatusScreen() {
    var isCollecting by remember { mutableStateOf(true) }
    var elapsedTime by remember { mutableStateOf(0) }

    LaunchedEffect(isCollecting) {
        while (isCollecting) {
            delay(1000)
            elapsedTime += 1
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
    ) {
        Text(
            text = "자세 모니터링",
            style = WearMaterialTheme.typography.title3,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "동작 중: ${formatTime(elapsedTime)}",
            style = WearMaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { isCollecting = !isCollecting },
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text(text = if (isCollecting) "중지" else "재시작")
        }
    }
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun defaultPreview() {
    postureMonitorApp()
}
