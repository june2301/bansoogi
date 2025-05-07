package com.ddc.bansoogi.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.tooling.preview.devices.WearDevices
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ddc.bansoogi.R

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(GifDecoder.Factory())
                add(ImageDecoderDecoder.Factory())
            }
            .build()
    }

    var progressValue by remember { mutableIntStateOf(40) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BackgroundImage()
        HeaderButtons()
        BansoogiContent(imageLoader, progressValue)
    }
}

@Composable
fun BackgroundImage() {
    Image(
        painter = painterResource(id = R.drawable.background_sunny_sky),
        contentDescription = "배경 하늘",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun HeaderButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconCircleButton(R.drawable.cookie) {
            // TODO: 왼쪽 버튼 클릭 이벤트
        }
        IconCircleButton(R.drawable.dehaze) {
            // TODO: 오른쪽 버튼 클릭 이벤트
        }
    }
}

@Composable
fun IconCircleButton(iconResId: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(36.dp)
            .background(Color.White, shape = RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = "icon",
            modifier = Modifier
                .width(20.dp)
                .height(20.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun BansoogiContent(imageLoader: ImageLoader, progressValue: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(R.drawable.bansoogi_basic)
                    .build(),
                imageLoader = imageLoader
            ),
            contentDescription = "반숙이",
            modifier = Modifier
                .size(140.dp)
                .offset(x = 8.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(8.dp))

        EnergyBar(progressValue)
    }
}

@Composable
fun EnergyBar(progressValue: Int) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(24.dp)
            .background(Color(0xFFEEEEEE))
            .border(width = 2.dp, color = Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progressValue / 100f)
                .fillMaxHeight()
                .background(Color(0xFF4CAF50))
        )

        Text(
            text = "$progressValue / 100",
            modifier = Modifier.align(Alignment.Center),
            color = Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MainScreen()
}