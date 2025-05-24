package com.ddc.bansoogi.landing.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddc.bansoogi.common.util.time.validateTime

@Composable
fun TimeInputTextField(
    timeState: MutableState<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onTimeChange: (String) -> Unit = {}
) {
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var hasFocus by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = timeState.value,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(4)
            timeState.value = digits
            onTimeChange(digits)

            if (digits.length == 4 && validateTime(digits)) {
                val formatted = "%02d:%02d".format(digits.take(2).toInt(), digits.drop(2).toInt())
                timeState.value = formatted
                onTimeChange(formatted)
            } else if (digits.length == 4) {
                errorMessage = "시간 형식이 잘못되었습니다. (시: 0~23, 분: 0~59)"
                showErrorDialog = true
            }
        },
        modifier = modifier
            .onFocusChanged { focusState ->
                if (hasFocus && !focusState.isFocused) {
                    val digits = timeState.value.filter { it.isDigit() }

                    if (digits.length != 4) {
                        errorMessage = "시간은 4자리로 입력해야 합니다."
                        showErrorDialog = true
                    }
                }

                hasFocus = focusState.isFocused
            }
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .background(Color.White)
            .fillMaxWidth(),
        enabled = enabled,
        textStyle = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        ),
        placeholder = {
            Text(
                "00:00",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                timeState.value = ""
                onTimeChange("")
            },
            confirmButton = {
                Text(
                    "확인",
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            showErrorDialog = false
                            timeState.value = ""
                            onTimeChange("")
                        }
                )
            },
            title = { Text("입력 오류") },
            text = { Text(errorMessage) }
        )
    }
}
