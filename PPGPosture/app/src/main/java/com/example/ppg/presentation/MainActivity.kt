package com.example.ppg.presentation

import android.content.BroadcastReceiver     // ← 빠져있음
import android.content.IntentFilter
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.*
import com.example.ppg.presentation.theme.PPGPostureTheme
import com.example.ppg.sensor.RecorderService
import kotlinx.coroutines.delay
import java.time.LocalTime

private enum class UiState { SELECT, PREPARE, RECORD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { WearApp() }
    }
}

@Composable
private fun WearApp() {
    PPGPostureTheme {
        val ctx = LocalContext.current
        val postures = listOf("standing", "upright‑sitting", "supine‑lying")
        val modelFiles = ctx.assets.list("models")?.sorted() ?: emptyList()

        var uiState by rememberSaveable { mutableStateOf(UiState.SELECT) }
        var lastStop by rememberSaveable { mutableStateOf("") }
        var modelName by rememberSaveable { mutableStateOf(modelFiles.firstOrNull() ?: "") }

        val posturePicker = rememberPickerState(postures.size, 0)
        val modelPicker = rememberPickerState(modelFiles.size, 0)

        val inference = remember { mutableStateOf("…") }

        DisposableEffect(Unit) {
            val br = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, i: Intent?) {
                    inference.value = when (i?.getIntExtra("prediction", -1) ?: -1) {
                        0 -> "upright‑sitting"
                        1 -> "supine‑lying"
                        2 -> "standing"
                        else -> "…"
                    }
                }
            }
            ctx.registerReceiver(br, IntentFilter(RecorderService.ACTION_PREDICTION), Context.RECEIVER_NOT_EXPORTED)
            onDispose { ctx.unregisterReceiver(br) }
        }

        val androidPerms = arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.HIGH_SAMPLING_RATE_SENSORS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_HEALTH
        )
        fun hasAllPerms() = androidPerms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
        val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            if (res.values.all { it }) uiState = UiState.PREPARE
            else Toast.makeText(ctx, "센서 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }

        when (uiState) {
            UiState.SELECT -> SelectScreen(postures, posturePicker, modelFiles, modelPicker, lastStop) {
                if (modelFiles.isEmpty()) { Toast.makeText(ctx, "모델 파일 없음", Toast.LENGTH_SHORT).show(); return@SelectScreen }
                modelName = modelFiles[modelPicker.selectedOption]
                if (!hasAllPerms()) { permLauncher.launch(androidPerms); return@SelectScreen }
                uiState = UiState.PREPARE
            }
            UiState.PREPARE -> CountdownScreen(3) {
                ContextCompat.startForegroundService(ctx, Intent(ctx, RecorderService::class.java).apply {
                    action = RecorderService.ACTION_START
                    putExtra("label", postures[posturePicker.selectedOption])
                    putExtra("model", modelName)
                })
                inference.value = "…"
                uiState = UiState.RECORD
            }
            UiState.RECORD -> RecordScreen(postures[posturePicker.selectedOption], 180, inference.value) {
                ContextCompat.startForegroundService(ctx, Intent(ctx, RecorderService::class.java).apply { action = RecorderService.ACTION_STOP })
                lastStop = LocalTime.now().withNano(0).toString()
                uiState = UiState.SELECT
            }
        }
    }
}

@Composable
private fun SelectScreen(
    postures: List<String>,
    posturePicker: PickerState,
    models: List<String>,
    modelPicker: PickerState,
    lastStop: String,
    onStart: () -> Unit
) {
    // 컴포지션 시에 한 번만 LocalContext.current 호출
    val ctx = LocalContext.current

    Scaffold(timeText = { TimeText() }) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Picker(state = posturePicker, modifier = Modifier.height(80.dp)) { ix ->
                Text(
                    text = postures[ix],
                    textAlign = TextAlign.Center
                )
            }
            Picker(state = modelPicker, modifier = Modifier.height(60.dp)) { ix ->
                Text(
                    text = models[ix]
                        .removePrefix("ppg_")
                        .removeSuffix(".tflite"),
                    textAlign = TextAlign.Center
                )
            }
            Button(onClick = onStart) {
                Text("시작")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                // 여기서는 이미 캡처된 ctx를 사용
                ctx.startActivity(
                    Intent(ctx, MainActivityCalibAndLive::class.java)
                )
            }) {
                Text("Calibrate & Live‑Test")
            }
            if (lastStop.isNotEmpty()) {
                Text(
                    text = "마지막 종료: $lastStop",
                    modifier = Modifier.padding(top = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ---------------------------------------------------------- */
@Composable
fun CountdownScreen(sec: Int, onFinish: () -> Unit) {
    var remain by remember { mutableStateOf(sec) }
    LaunchedEffect(Unit) {
        for (s in sec downTo 1) { remain = s; delay(1_000) }
        onFinish()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$remain", style = MaterialTheme.typography.display1)
    }
}

@Composable
fun RecordScreen(
    label: String,
    remainInit: Int,
    inference: String,
    onDone: () -> Unit
) {
    var remain by remember { mutableStateOf(remainInit) }
    LaunchedEffect(Unit) {
        for (s in remainInit downTo 1) { remain = s; delay(1_000) }
        onDone()
    }
    Scaffold(timeText = { TimeText() }) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("남은 시간: ${"%03d".format(remain)}s")
            Spacer(Modifier.height(4.dp))
            Text("Recording: $label")
            Spacer(Modifier.height(8.dp))
            Text("Inference: $inference", style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDone) { Text("취소") }
        }
    }
}
