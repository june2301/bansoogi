package com.example.eggi.main.ui.manage

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.eggi.R
import com.example.eggi.myInfo.data.model.MyInfo

@Composable
fun EggManagerModal(
    myInfo: MyInfo?,
    onDismiss: () -> Unit
) {
    val nickname = myInfo?.nickname ?: "Guest"

    // 0: 깨지지 않은 달걀, 1: 깨지는 달걀, 2: 캐릭터 표시!
    val eggState = remember { mutableStateOf(0) }
    val isLoading = remember { mutableStateOf(true) }
    val alpha = animateFloatAsState(
        targetValue = if (isLoading.value) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Dialog(
        onDismissRequest = { /* 닫기 방지 - 알을 받아야만 닫을 수 있음 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize().alpha(alpha.value)) {
                val context = LocalContext.current
                val imageLoader = remember {
                    ImageLoader.Builder(context)
                        .components {
                            add(GifDecoder.Factory())
                            add(ImageDecoderDecoder.Factory())
                        }
                        .build()
                }

                // 이미지 미리 로딩
                LaunchedEffect(Unit) {
                    val preloadRequests = listOf(
                        R.drawable.background_fan_1,
                        R.drawable.background_fan_2,
                        R.drawable.egg_before_broken,
                        R.drawable.egg_breaking,
                        R.drawable.bansoogi_basic
                    )
                    preloadRequests.forEach { resource ->
                        imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .data(resource)
                                .build()
                        )
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        isLoading.value = false
                    }, 300)
                }

                val backgroundImage = when (eggState.value) {
                    0 -> R.drawable.background_fan_1
                    else -> R.drawable.background_fan_2
                }
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(backgroundImage)
                            .build(),
                        imageLoader = imageLoader
                    ),
                    contentDescription = "배경 주방",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillHeight
                )
                when (eggState.value) {
                    0 -> {
                        EggBeforeBroken(nickname, eggState)
                    }
                    1 -> {
                        EggBreakingAnimation(eggState)
                    }
                    2 -> {
                        EggCharacterReceived(onDismiss)
                    }
                }
            }
        }
    }
}
