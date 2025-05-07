package com.ddc.bansoogi.landing.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder

@Composable
fun GifImage(imageModel: Any?, description: String, modifier: Modifier) {

    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(GifDecoder.Factory())
        }
        .build()

    AsyncImage(
        model = imageModel,
        contentDescription = description,
        imageLoader = imageLoader,
        modifier = modifier
    )

}