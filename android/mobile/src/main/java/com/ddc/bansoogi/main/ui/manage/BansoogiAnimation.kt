package com.ddc.bansoogi.main.ui.manage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import com.ddc.bansoogi.common.ui.component.SpriteSheetAnimation
import com.ddc.bansoogi.main.ui.util.BansoogiState
import com.ddc.bansoogi.main.ui.util.getConfig

@Composable
fun BansoogiAnimation(
    state: BansoogiState,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var config = state.getConfig()

    SpriteSheetAnimation(
        context = context,
        spriteSheetName = "${config.sprite}_sheet.png",
        jsonName = "${config.json}.json",
        modifier = Modifier
            .fillMaxSize()
            .scale(1.5f)
    )
}