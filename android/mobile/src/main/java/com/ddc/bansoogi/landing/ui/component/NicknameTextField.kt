package com.ddc.bansoogi.landing.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun NicknameTextField(
    letterCount: Int,
    nicknameState: MutableState<String>,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = nicknameState.value,
        onValueChange = { input ->
            val noSpace = input.replace(" ", "")

            if (noSpace.length <= letterCount && noSpace.all {
                    it.isDigit() || it.isAsciiLetter() || it.isCompleteHangul()
                }
            ) {
                nicknameState.value = noSpace
            }
        },
        placeholder = { Text("닉네임") },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.None
        )
    )
}

fun Char.isAllowedChar(): Boolean {
    return isDigit() || isAsciiLetter() || isCompleteHangul()
}

fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

fun Char.isCompleteHangul(): Boolean = this in '\uAC00'..'\uD7A3'