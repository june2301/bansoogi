package com.example.eggi.main.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.eggi.R
import com.example.eggi.main.controller.TodayRecordController
import com.example.eggi.myInfo.data.model.MyInfo

@Composable
fun EggManagerModal(
    myInfo: MyInfo?,
    todayRecordController: TodayRecordController,
    onDismiss: () -> Unit
) {
    val nickname = myInfo?.nickname ?: "Guest"

    Dialog(
        onDismissRequest = { /* 닫기 방지 - 알을 받아야만 닫을 수 있음 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                            .data(R.drawable.background_cooked)
                            .build(),
                        imageLoader = imageLoader
                    ),
                    contentDescription = "배경 주방",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillHeight
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(150.dp))

                    Text(
                        text = "환영합니다 $nickname 님!"
                    )
                    Text(
                        text = "오늘의 달걀을 받아보세요!"
                    )
                    Image(
                        painter = painterResource(id = R.drawable.egg_before_broken),
                        contentDescription = "깨지기 전 달걀",
                    )
                    Button(
                        onClick = {
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "알 받기"
                        )
                    }
                }
            }
        }
    }
}
