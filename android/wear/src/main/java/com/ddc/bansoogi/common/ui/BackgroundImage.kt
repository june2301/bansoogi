package com.ddc.bansoogi.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import com.ddc.bansoogi.R

@Composable
fun BackgroundImage() {
    Image(
        painter = painterResource(R.drawable.background_sunny_sky),
        contentDescription = "배경 하늘",
        modifier = Modifier
            .fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun BackgroundPreview() {
    BackgroundImage()
}