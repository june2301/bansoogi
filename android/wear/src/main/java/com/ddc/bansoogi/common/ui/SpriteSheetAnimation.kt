package com.ddc.bansoogi.common.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AsepriteFrameData(val duration: Int)

@Serializable
data class AsepriteJson(val frames: Map<String, AsepriteFrameEntry>)

@Serializable
data class AsepriteFrameEntry(val frame: FramePos, val duration: Int)

@Serializable
data class FramePos(val x: Int, val y: Int)

@Composable
fun SpriteSheetAnimation(
    context: Context,
    spriteSheetName: String,
    jsonName: String,
    loop: Boolean = true,
    loopCount: Int = 0,
    onAnimationEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 1. JSON 로드 및 파싱
    val frames = remember(spriteSheetName, jsonName) {
        val jsonText = context.assets.open(jsonName).bufferedReader().use { it.readText() }
        Json { ignoreUnknownKeys = true }
            .decodeFromString<AsepriteJson>(jsonText).frames.values.map {
                Pair(it.frame, it.duration)
            }
    }

    // 2. sprite sheet bitmap 로드
    val fullBitmap = remember(spriteSheetName) {
        context.assets.open(spriteSheetName).use {
            BitmapFactory.decodeStream(it)
        }
    }

    // 3. 애니메이션 인덱스 관리
    var frameIndex by remember { mutableStateOf(0) }

    // 4. 주기적 프레임 변경
    var currentLoop by remember { mutableStateOf(0) } // 루프 횟수 상태

    LaunchedEffect(spriteSheetName, jsonName, loop) {
        frameIndex = 0
        currentLoop = 0

        if (loop) {
            while (true) {
                delay(frames[frameIndex].second.toLong())
                frameIndex = (frameIndex + 1) % frames.size
            }
        } else {
            outer@ while (currentLoop < loopCount) {
                for ((i, frame) in frames.withIndex()) {
                    frameIndex = i
                    delay(frame.second.toLong())
                }
                currentLoop++
            }
            onAnimationEnd?.invoke()
        }
    }

    // 5. 현재 프레임 추출 (128x128 고정)
    val (pos, _) = remember(frameIndex, frames) {
        frames.getOrNull(frameIndex) ?: frames.first()
    }
    val croppedBitmap = remember(frameIndex) {
        Bitmap.createBitmap(fullBitmap, pos.x, pos.y, 128, 128).asImageBitmap()
    }

    Image(
        bitmap = croppedBitmap,
        contentDescription = null,
        modifier = modifier
    )
}