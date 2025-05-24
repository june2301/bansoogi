package com.ddc.bansoogi.main.ui.manage

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ddc.bansoogi.R
import com.ddc.bansoogi.myInfo.data.model.MyInfoDto

@Composable
fun EggManagerModal(
    myInfo: MyInfoDto?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {

    val nickname = myInfo?.nickname ?: "Guest"
    val eggState = remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val alpha = animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    // 이미지 로더 및 프리로드
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(GifDecoder.Factory())
                add(ImageDecoderDecoder.Factory())
            }
            .build()
    }
    LaunchedEffect(Unit) {
        listOf(
            R.drawable.background_fan_1,
            R.drawable.background_fan_2,
            R.drawable.egg_before_broken_gif,
            R.drawable.egg_breaking,
            R.drawable.bansoogi_basic
        ).forEach { res ->
            imageLoader.enqueue(ImageRequest.Builder(context).data(res).build())
        }
        Handler(Looper.getMainLooper()).postDelayed({ isLoading = false }, 300)
    }

    // 전체 화면 오버레이
    Box(
        modifier = modifier
            .background(Color.White)
            .alpha(alpha.value)
    ) {
        // 배경 이미지
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
            contentScale = ContentScale.Crop
        )

        // 달걀/캐릭터 상태
        when (eggState.value) {
            0 -> EggBeforeBroken(nickname, eggState)
            1 -> EggBreakingAnimation(eggState)
            2 -> EggCharacterReceived(onDismiss)
        }
    }
}
