package com.ddc.bansoogi.main.ui.manage

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ddc.bansoogi.R
import kotlinx.coroutines.delay

@Composable
fun EggBreakingAnimation(eggState: MutableState<Int>) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .components {
                    add(GifDecoder.Factory())
                    add(ImageDecoderDecoder.Factory())
                }
                .build()
        }

        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(R.drawable.egg_breaking)
                    .build(),
                imageLoader = imageLoader
            ),
            contentDescription = "달걀 깨지는 중",
            modifier = Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(1f)
                .padding(bottom = maxHeight * 0.25f),
            contentScale = ContentScale.Fit
        )

        LaunchedEffect(Unit) {
            delay(1920)
            eggState.value = 2
        }
    }
}

@Composable
fun EggCharacterReceived(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.37f))

        val context = LocalContext.current
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .components {
                    add(GifDecoder.Factory())
                    add(ImageDecoderDecoder.Factory())
                }
                .build()
        }
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(R.drawable.bansoogi_basic)
                    .build(),
                imageLoader = imageLoader
            ),
            contentDescription = "받은 캐릭터",
            modifier = Modifier
                .width(350.dp)
                .height(350.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "터치해서 시작하기 →",
            modifier = Modifier.clickable { onDismiss() },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.White,
                    offset = Offset(5f, 5f)
                )
            )
        )

        Spacer(modifier = Modifier.weight(0.18f))
    }
}