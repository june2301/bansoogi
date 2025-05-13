/*
 * MainActivity.kt – Wear Compose entry & permission flow
 */
package com.example.ppg.presentation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.ppg.presentation.theme.PPGTheme
import com.example.ppg.sensor.RecorderService
import java.time.LocalTime
import java.util.Set // for reflection

private enum class UiState { SELECT, PREPARE, RECORD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent { WearApp() }
    }
}

@Composable
private fun WearApp() {
    PPGTheme {
        // ----------  UI state ----------
        val postures =
            listOf(
                "서 있기 / standing",
                "바른 자세로 앉기 / upright-sitting",
                "반듯이 눕기 / supine-lying",
                "뒤로 기대기 / reclined",
                "앞으로 숙이기 / forward-lean",
                "다리 꼬고 앉기 / cross-legged",
                "배를 대고 누우기 / prone",
                "반좌위 / Fowler’s",
                "오른쪽 옆으로 누우기 / right-lat",
                "왼쪽 옆으로 누우기 / left-lat",
            )
        var uiState by rememberSaveable { mutableStateOf(UiState.SELECT) }
        var lastStop by rememberSaveable { mutableStateOf("") }
        val pickerState =
            rememberPickerState(
                initialNumberOfOptions = postures.size,
                initiallySelectedOption = 0,
            )

        // ----------  Context / Activity ----------
        val ctx = LocalContext.current
        val act = LocalActivity.current

        // ----------  Android runtime permissions ----------
        val androidPerms =
            arrayOf(
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.HIGH_SAMPLING_RATE_SENSORS,
            )

        fun hasAllAndroidPerms(): Boolean =
            androidPerms.all {
                ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
            }

        // ----------  Sensor-SDK permission (via reflection) ----------
        val ppgPermKey by remember {
            mutableStateOf(
                try {
                    val trackerTypeCls =
                        Class.forName(
                            "com.samsung.android.service.health.tracking.data.HealthTrackerType",
                        )
                    val permissionKeyCls =
                        Class.forName(
                            "com.samsung.android.service.health.tracking.permission." +
                                "HealthPermissionManager\$PermissionKey",
                        )
                    val ppgField = trackerTypeCls.getField("PPG_CONTINUOUS")
                    permissionKeyCls
                        .getConstructor(trackerTypeCls)
                        .newInstance(ppgField.get(null))
                } catch (_: Exception) {
                    null // SDK 미설치 기기 대비 fallback
                },
            )
        }

        fun hasSamsungPerm(
            activity: Activity?,
            key: Any?,
        ): Boolean =
            if (activity == null || key == null) {
                true
            } else {
                try {
                    val mgrCls =
                        Class.forName(
                            "com.samsung.android.service.health.tracking.permission.HealthPermissionManager",
                        )
                    val mgr = mgrCls.getConstructor(Activity::class.java).newInstance(activity)
                    val isGranted =
                        mgrCls
                            .getMethod("isPermissionAcquired", Set::class.java)
                            .invoke(mgr, setOf(key)) as Boolean
                    isGranted
                } catch (_: Exception) {
                /* 클래스가 없으면 OS가 permission UI를 자체적으로 제공하지 않는 기기.
                   Wear 5 이상은 기본적으로 허용해주므로 true 로 간주 */
                    true
                }
            }

        // ----------  Launchers ----------
        val androidPermLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { resultMap ->
                if (resultMap.values.all { it }) {
                    uiState = UiState.PREPARE
                } else {
                    Toast.makeText(ctx, "센서 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                }
            }

        val samsungPermLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { _ ->
                if (hasAllAndroidPerms() && hasSamsungPerm(act, ppgPermKey)) {
                    uiState = UiState.PREPARE
                } else {
                    Toast.makeText(ctx, "Samsung Health 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                }
            }

        // ----------  Screen Routing ----------
        when (uiState) {
            UiState.SELECT ->
                SelectScreen(
                    postures = postures,
                    picker = pickerState,
                    lastStop = lastStop,
                ) {
                    // ‘시작’ 버튼 콜백
                    if (!hasAllAndroidPerms()) {
                        androidPermLauncher.launch(androidPerms)
                        return@SelectScreen
                    }
                    if (!hasSamsungPerm(act, ppgPermKey)) {
                        try {
                            val mgrCls =
                                Class.forName(
                                    "com.samsung.android.service.health.tracking.permission." +
                                        "HealthPermissionManager",
                                )
                            val mgr =
                                mgrCls
                                    .getConstructor(Activity::class.java)
                                    .newInstance(act)
                            val intent =
                                mgrCls
                                    .getMethod(
                                        "createRequestPermissionIntent",
                                        Set::class.java,
                                    ).invoke(mgr, setOf(ppgPermKey)) as? Intent
                            intent?.let { samsungPermLauncher.launch(it) }
                                ?: Toast
                                    .makeText(
                                        ctx,
                                        "Samsung Health 권한 요청 실패",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                        } catch (_: Exception) {
                            Toast.makeText(ctx, "Samsung Health 권한 요청 실패", Toast.LENGTH_SHORT).show()
                        }
                        return@SelectScreen
                    }
                    uiState = UiState.PREPARE
                }

            UiState.PREPARE ->
                CountdownScreen(seconds = 3) {
                    ContextCompat.startForegroundService(
                        ctx,
                        Intent(ctx, RecorderService::class.java).apply {
                            action = RecorderService.ACTION_START
                            putExtra("label", postures[pickerState.selectedOption])
                        },
                    )
                    uiState = UiState.RECORD
                }

            UiState.RECORD ->
                RecordScreen(
                    label = postures[pickerState.selectedOption],
                    totalSec = 30,
                ) {
                    ContextCompat.startForegroundService(
                        ctx,
                        Intent(ctx, RecorderService::class.java).apply {
                            action = RecorderService.ACTION_STOP
                        },
                    )
                    lastStop = LocalTime.now().withNano(0).toString()
                    uiState = UiState.SELECT
                }
        }
    }
}

// ------------------------- Composable screens -------------------------

@Composable
private fun SelectScreen(
    postures: List<String>,
    picker: PickerState,
    lastStop: String,
    onStart: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        TimeText(modifier = Modifier.align(Alignment.TopCenter))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
        ) {
            Picker(
                state = picker,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) { ix ->
                Text(
                    text = postures[ix],
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onStart,
            ) { Text("시작") }
        }
        if (lastStop.isNotEmpty()) {
            Text(
                text = "마지막 측정 종료: $lastStop",
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                color = MaterialTheme.colors.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CountdownScreen(
    seconds: Int,
    onFinished: () -> Unit,
) {
    var remaining by remember { mutableStateOf(seconds) }
    LaunchedEffect(Unit) {
        for (i in seconds downTo 1) {
            remaining = i
            kotlinx.coroutines.delay(1000)
        }
        onFinished()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$remaining",
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RecordScreen(
    label: String,
    totalSec: Int,
    onDone: () -> Unit,
) {
    var remain by remember { mutableStateOf(totalSec) }
    LaunchedEffect(Unit) {
        for (s in totalSec downTo 1) {
            remain = s
            kotlinx.coroutines.delay(1000)
        }
        onDone()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        TimeText(modifier = Modifier.align(Alignment.TopCenter))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("남은 시간: ${"%02d".format(remain)}s", color = MaterialTheme.colors.primary)
            Text("Recording $label…", color = MaterialTheme.colors.primary)
            Button(onClick = onDone) { Text("취소") }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun PreviewWearApp() = WearApp()
