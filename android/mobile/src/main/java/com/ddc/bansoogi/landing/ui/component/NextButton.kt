package com.ddc.bansoogi.landing.ui.component

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
fun NextButton(
    text: String = "다음",
    enabled: Boolean = true,
    onClick: () -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    contentButtonColor: Color = Color(0xFF4CABFD),
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentButtonColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.LightGray
        ),
        modifier = modifier
    ) {
        Text(text, style = textStyle)
    }
}