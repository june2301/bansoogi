package com.ddc.bansoogi.common.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import com.ddc.bansoogi.R
import com.ddc.bansoogi.main.ui.util.BansoogiStateHolder

@Composable
fun BackgroundImage() {
    var currentBackgroundResId by remember{ mutableIntStateOf(R.drawable.background_sunny_sky) }

    LaunchedEffect(BansoogiStateHolder.state) {
        currentBackgroundResId = BansoogiStateHolder.background()
    }

    AnimatedContent(
        targetState = currentBackgroundResId,
        transitionSpec = {
            slideInHorizontally(
                animationSpec = tween(durationMillis = 1000),
                initialOffsetX = { -it } // 위에서 아래로 등장
            ) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = 1000),
                        targetOffsetX = { it }   // 아래로 밀려서 퇴장
                    )
        }

    ) { resId ->
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun BackgroundPreview() {
    BackgroundImage()
}