package com.ddc.bansoogi.landing.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RoundedContainerBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    borderColor: Color = Color.Black,
    cornerRadius: Int = 24,
    borderWidth: Int = 4,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .border(borderWidth.dp, borderColor, RoundedCornerShape(cornerRadius.dp))
            .background(backgroundColor)
            .fillMaxWidth(),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}