package com.ddc.bansoogi.landing.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun NicknameTextField(
    letterCount: Int,
    nicknameState: MutableState<String>,
    backgroundColor: Color = Color.White,
    borderColor: Color = Color.Black,
    cornerRadius: Int = 12,
    borderWidth: Int = 1,
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
        placeholder = { Text("3~8자, 한글, 숫자, 영어") },
        singleLine = true,

        //MAKR: borderColor 같은것이 미리 입력받아진다면, 예외처리해서 해야할 것
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .border(borderWidth.dp, borderColor, RoundedCornerShape(cornerRadius.dp))
            .background(backgroundColor),

        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.None
        )
    )
}

//MARK: 텍스트 입력값 함수
private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
private fun Char.isCompleteHangul(): Boolean = this in '\uAC00'..'\uD7A3'
private fun Char.isAllowedChar(): Boolean {
    return isDigit() || isAsciiLetter() || isCompleteHangul()
}