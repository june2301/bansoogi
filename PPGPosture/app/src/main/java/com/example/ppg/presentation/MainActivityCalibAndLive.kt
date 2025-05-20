package com.example.ppg.presentation

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.*
import com.example.ppg.sensor.CalibRuleService
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class MainActivityCalibAndLive : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { WearApp() }
    }
}

private enum class TabPage { CALIB, LIVE }
private enum class CalibState { NONE, RECORDING, DONE }
private val POSTURES = listOf("upright-sitting", "supine-lying", "standing")

@Composable
private fun WearApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val perms = arrayOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.HIGH_SAMPLING_RATE_SENSORS,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.FOREGROUND_SERVICE_HEALTH
    )
    var granted by remember { mutableStateOf(false) }
    fun checkPerm() = perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        granted = res.values.all { it }
    }
    if (!granted) granted = checkPerm()

    var tab by remember { mutableStateOf(TabPage.CALIB) }

    Scaffold(timeText = { TimeText() }) {
        when (tab) {
            TabPage.CALIB -> CalibScreen(ctx, granted, { permLauncher.launch(perms) })
            TabPage.LIVE  -> LiveScreen(ctx) { tab = TabPage.CALIB }
        }
    }
}

@Composable
private fun CalibScreen(
    ctx: Context,
    hasPerm: Boolean,
    reqPerm: () -> Unit
) {
    val calibFile = File(ctx.filesDir, "calib.json")
    var recordingLabel by remember { mutableStateOf<String?>(null) }

    // compute state per posture
    fun stateFor(p: String): CalibState {
        return when {
            recordingLabel == p -> CalibState.RECORDING
            calibFile.exists() && JSONObject(calibFile.readText()).optJSONObject("raw")?.optJSONArray(p)?.length() ?: 0 > 0 -> CalibState.DONE
            else -> CalibState.NONE
        }
    }

    DisposableEffect(Unit) {
        val br = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                val lab = i?.getStringExtra("label") ?: return
                if (lab == recordingLabel) recordingLabel = null
            }
        }
        ctx.registerReceiver(br, IntentFilter(CalibRuleService.ACTION_CALIB_DONE), Context.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.unregisterReceiver(br) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Calibration", style = MaterialTheme.typography.title3)
        Spacer(Modifier.height(8.dp))
        POSTURES.forEach { posture ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(posture.replace("-", "\n"), textAlign = TextAlign.Center)
                Spacer(Modifier.width(8.dp))
                when (stateFor(posture)) {
                    CalibState.NONE -> Button(onClick = {
                        if (!hasPerm) { reqPerm(); return@Button }
                        recordingLabel = posture
                        ctx.startForegroundService(Intent(ctx, CalibRuleService::class.java).apply {
                            action = CalibRuleService.ACTION_CALIB
                            putExtra("label", posture)
                        })
                        Toast.makeText(ctx, "65s recording…", Toast.LENGTH_SHORT).show()
                    }) { Text("Rec") }
                    CalibState.RECORDING -> Text("...")
                    CalibState.DONE -> Button(onClick = {
                        // reset
                        JSONObject(calibFile.readText()).let { root ->
                            root.optJSONObject("raw")?.remove(posture)
                            calibFile.writeText(root.toString())
                        }
                    }) { Text("Reset") }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (POSTURES.all { stateFor(it) == CalibState.DONE }) {
            // Launch the MainActivity when all calibrations are DONE directly from a composable context
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}

@Composable
private fun LiveScreen(
    ctx: Context,
    onBack: () -> Unit
) {
    val rule = remember { mutableStateOf("…") }

    DisposableEffect(Unit) {
        ctx.startForegroundService(Intent(ctx, CalibRuleService::class.java).apply { action = CalibRuleService.ACTION_LIVE })
        val br = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                rule.value = i?.getStringExtra("rule_label") ?: "…"
            }
        }
        ctx.registerReceiver(br, IntentFilter(CalibRuleService.ACTION_RULE), Context.RECEIVER_NOT_EXPORTED)
        onDispose {
            ctx.startForegroundService(Intent(ctx, CalibRuleService::class.java).apply { action = CalibRuleService.ACTION_STOP })
            ctx.unregisterReceiver(br)
        }
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Rule Inference", style = MaterialTheme.typography.title3)
        Spacer(Modifier.height(8.dp))
        Text(rule.value, style = MaterialTheme.typography.display1)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            // navigate to ML‑based measurement screen
            val intent = Intent(ctx, MainActivity::class.java)
            ctx.startActivity(intent)
        }) { Text("ML Measure") }
    }
}