//package com.ddc.bansoogi.common.ui.component
//
//import androidx.compose.foundation.Image
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import coil.ImageLoader
//import coil.compose.rememberAsyncImagePainter
//import coil.decode.GifDecoder
//import coil.decode.ImageDecoderDecoder
//import coil.request.ImageRequest
//
//@Composable
//fun BansoogiAnimation(
//    resource: Int,
//    description: String,
//    modifier: Modifier = Modifier,
//    scale: Float = 1.5f,
//    contentScale: ContentScale = ContentScale.Fit
//) {
//    val context = LocalContext.current
//    val imageLoader = remember {
//        ImageLoader.Builder(context)
//            .components {
//                add(GifDecoder.Factory())
//                add(ImageDecoderDecoder.Factory())
//            }
//            .build()
//    }
//    val painter = rememberAsyncImagePainter(
//        ImageRequest.Builder(context)
//            .data(resource)
//            .build(),
//        imageLoader = imageLoader
//    )
//
//    Image(
//        painter = painter,
//        contentDescription = description,
//        modifier = modifier
//            .then(
//                Modifier.scale(scale)
//            ),
//        contentScale = contentScale
//    )
//}