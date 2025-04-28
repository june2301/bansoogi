package com.example.prototype.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.prototype.ProtoWearSensorService
import com.example.prototype.presentation.theme.protoTypeTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        // 센서 서비스 시작
        val serviceIntent = Intent(this, ProtoWearSensorService::class.java)
        startService(serviceIntent)

        setContent {
            postureMonitorApp()
        }
    }
}

@Composable
fun postureMonitorApp() {
    protoTypeTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
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

    LaunchedEffect(key1 = isCollecting) {
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
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "동작 중: ${formatTime(elapsedTime)}",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground,
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
