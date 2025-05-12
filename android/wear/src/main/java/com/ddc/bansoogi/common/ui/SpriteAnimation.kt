package com.ddc.bansoogi.common.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.ddc.bansoogi.R

@Composable
fun SpriteAnimation(
    spriteResource: Int,
    modifier: Modifier = Modifier,
    frameWidth: Int = 128, // 프레임 규격입니다.
    frameHeight: Int = 128, // 프레임 규격입니다.
    totalFrames: Int = 53, // 프레임 수입니다. 파일마다 달라지니 확인해서 바꿔주세요!
    frameDurationMillis: Int = 200, // duration인데 이것도 파일마다 달라지니 확인해주세요!
    loop: Boolean = true, // 루프 여부 설정
    onAnimationFinished: (() -> Unit)? = null // 애니메이션이 끝나면 콜백
) {
    val spriteSheet = ImageBitmap.imageResource(id = spriteResource)
    var currentFrame by remember { mutableStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }

    val frameLimit = spriteSheet.width / frameWidth
    val validFrames = minOf(totalFrames, frameLimit)

    LaunchedEffect(Unit) {
        while (!isFinished) {
            delay(frameDurationMillis.toLong())
            if (currentFrame == validFrames - 1) {
                if (loop) {
                    currentFrame = 0
                } else {
                    isFinished = true
                    onAnimationFinished?.invoke()
                }
            } else {
                currentFrame++
            }
        }
    }

    Canvas(
        modifier = modifier
            .size(frameWidth.dp, frameHeight.dp)
            .scale(1.3f)
    ) {
        drawImage(
            image = spriteSheet,
            srcOffset = IntOffset(currentFrame * frameWidth, 0),
            srcSize = IntSize(frameWidth, frameHeight),
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
    }
}