package com.ddc.bansoogi.landing.ui.component

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun NicknameTextField(
    letterCount: Int,
    nicknameState: MutableState<String>,
    onValidationChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var error by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = nicknameState.value,
        onValueChange = { input ->
            val noSpace = input.replace(" ", "")

            if (noSpace.length <= letterCount) {
                nicknameState.value = noSpace

                val isValid = noSpace.length in 3..letterCount &&
                        noSpace.all { it.isAllowedChar() }
                if (!isValid) {
                    error = "3~8자 완성형 한글, 영어, 숫자만 입력 가능해요"
                } else {
                    error = null
                }

                onValidationChanged(isValid)
            }
        },
        placeholder = { Text("3~8자, 한글, 숫자, 영어") },
        isError = error != null,
        supportingText = {
            error?.let { Text(it, color = Color.Red) }
        },
        singleLine = true,
        modifier = modifier
    )
}

private fun Char.isAsciiLetter(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z'

private fun Char.isCompleteHangul(): Boolean =
    this in '\uAC00'..'\uD7A3'

private fun Char.isAllowedChar(): Boolean =
    isDigit() || isAsciiLetter() || isCompleteHangul()