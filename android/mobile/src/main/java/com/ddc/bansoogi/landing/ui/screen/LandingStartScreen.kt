package com.ddc.bansoogi.landing.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddc.bansoogi.R
import com.ddc.bansoogi.landing.ui.component.GifImage

@Composable
fun LandingStartScreen(onNext: () -> Unit) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onNext() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.bansoogi_intro_logo),
                contentDescription = "반숙이 로고",
                contentScale = ContentScale.Crop
            )

            GifImage(
                R.drawable.bansoogi_walk,
                "landing bansoogi",
                Modifier.aspectRatio(ratio = 1f)
                .height(256.dp)
            )

            Text("눌러서 시작하기", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null, // ripple 제거
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onNext()
                }
        )
    }
}
