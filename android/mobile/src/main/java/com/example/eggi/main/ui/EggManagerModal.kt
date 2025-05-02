package com.example.eggi.main.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.eggi.main.controller.TodayRecordController

@Composable
fun EggManagerModal(
    todayRecordController: TodayRecordController,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* 닫기 방지 - 알을 받아야만 닫을 수 있음 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = Color.White
        ) {
            Button(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(
                    text = "알 받기"
                )
            }
        }
    }
}