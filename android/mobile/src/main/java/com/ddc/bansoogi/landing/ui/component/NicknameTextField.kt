package com.ddc.bansoogi.landing.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddc.bansoogi.common.util.extend.char.isAllowedChar

@Composable
fun NicknameTextField(
    letterCount: Int,
    nicknameState: MutableState<String>,
    onValidationChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {

        OutlinedTextField(
            value = nicknameState.value,
            onValueChange = { input ->
                val noSpace = input.replace(" ", "")

                if (noSpace.length <= letterCount) {
                    nicknameState.value = noSpace

                    val isValid = noSpace.length in 3..letterCount &&
                            noSpace.all { it.isAllowedChar() }

                    error = if (isValid) null
                    else "3~8자 완성형 한글, 영어, 숫자만 입력 가능해요"

                    onValidationChanged(isValid)
                }
            },
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            ),
            placeholder = {
                Text(
                    "3~8자, 한글, 숫자, 영어",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            },
            isError = error != null,
            singleLine = true,
            modifier = Modifier
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .background(Color.White)
                .fillMaxWidth(),
            shape  = RoundedCornerShape(0.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor  = Color.White,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor  = Color.Transparent,
                errorIndicatorColor     = Color.Transparent
            )
        )

        error?.let {
            Text(
                text = it,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}
