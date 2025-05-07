package com.ddc.bansoogi.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.wear.tooling.preview.devices.WearDevices
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ddc.bansoogi.R
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.ui.BackgroundImage
import com.ddc.bansoogi.common.ui.VerticalSpacer

@Composable
fun MainScreen(navController: NavHostController) {
    var progressValue by remember { mutableIntStateOf(80) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BackgroundImage()

        SideButtons(
            navController = navController
        )

        BansoogiContent(
            progressValue = progressValue
        )
    }
}

@Composable
fun SideButtons(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconCircleButton(
            iconResource = R.drawable.cookie,
            description = "밥 먹기 버튼"
        ) {
            // TODO: 왼쪽 버튼 클릭 이벤트
        }
        IconCircleButton(
            iconResource = R.drawable.dehaze,
            description = "메뉴 버튼"
        ) {
            navController.navigate(NavRoutes.MENU)
        }
    }
}

@Composable
fun IconCircleButton(iconResource: Int, description: String, onBtnClick: () -> Unit) {
    // 셀 클릭 시, 회색 창 제거
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(36.dp)
            .height(36.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(50)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // 리플 효과(회색 창) 제거
                onClick = onBtnClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconResource),
            contentDescription = description,
            modifier = Modifier
                .width(20.dp)
                .height(20.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun BansoogiContent(progressValue: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BansoogiAnimation(
            bansoogiResource = R.drawable.bansoogi_basic
        )

        VerticalSpacer()

        EnergyBar(
            progressValue = progressValue
        )
    }
}

@Composable
fun BansoogiAnimation(bansoogiResource: Int) {
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
            ImageRequest.Builder(LocalContext.current)
                .data(bansoogiResource)
                .build(),
            imageLoader = imageLoader
        ),
        contentDescription = "반숙이",
        modifier = Modifier
            .size(140.dp)
            .scale(1.3f)
            .offset(x = 8.dp, y = (-8).dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun EnergyBar(progressValue: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.65f)
            .height(12.dp)
            .background(
                color = Color.White,
            )
            .border(
                width = 2.dp,
                color = Color.DarkGray,
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progressValue / 100f)
                .fillMaxHeight()
                .background(Color.Green)
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MainScreen(rememberNavController())
}