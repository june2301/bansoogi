/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.ppg.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.ppg.R
import com.example.ppg.presentation.theme.PPGTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp("Android")
        }
    }
}

@Composable
fun WearApp(appName: String) {
    PPGTheme {
        val options = listOf(
            "서 있기 / standing",
            "바른 자세로 앉기 / upright-sitting",
            "반듯이 눕기 / supine-lying",
            "뒤로 기대기 / reclined",
            "앞으로 숙이기 / forward-lean",
            "다리를 꼬고 앉기 / cross-legged",
            "배를 대고 누우기 / prone",
            "반좌위 / Fowler’s",
            "오른쪽 옆으로 누우기 / right-lat",
            "왼쪽 옆으로 누우기 / left-lat"
        )

        val pickerState = rememberPickerState(
            initialNumberOfOptions = options.size,
            initiallySelectedOption = 0
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText(modifier = Modifier.align(Alignment.TopCenter))
            Picker(
                state = pickerState,
                modifier = Modifier.fillMaxWidth(),
            ) { index ->
                Text(
                    text = options[index],
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("PPG")
}