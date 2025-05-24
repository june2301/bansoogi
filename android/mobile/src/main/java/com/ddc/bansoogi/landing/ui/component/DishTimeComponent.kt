package com.ddc.bansoogi.landing.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun DishTimeComponent(
    title: String,
    initialToggleState: Boolean = false,
    timeState: MutableState<String>,
    onToggleChanged: (Boolean) -> Unit,
    onTimeChanged: (String) -> Unit
) {

    var isEnabled by remember { mutableStateOf(initialToggleState) }
    var time by remember { timeState }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.width(8.dp))

            TimeInputTextField(
                timeState = remember { mutableStateOf(time) },
                modifier = Modifier.fillMaxWidth(1f),
                enabled = isEnabled,
                onTimeChange = {
                    time = it
                    onTimeChanged(it)
                }
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .height(8.dp)
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                val dotSpacing = 30f
                val dotRadius = 10f
                var x = 0f
                while (x < size.width) {
                    drawCircle(
                        color = Color.Gray,
                        radius = dotRadius,
                        center = Offset(x, size.height / 2)
                    )
                    x += dotSpacing
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = {
                    isEnabled = it
                    onToggleChanged(it)
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF99CC00),
                    uncheckedTrackColor = Color(0xFFECECEC)
                )
            )
        }
    }
}