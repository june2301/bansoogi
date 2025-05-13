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
    modifier: Modifier = Modifier
) {
    // 1. JSON 로드 및 파싱
    val json = remember {
        Json { ignoreUnknownKeys = true }
    }
    val frames = remember {
        val jsonText = context.assets.open(jsonName).bufferedReader().use { it.readText() }
        json.decodeFromString<AsepriteJson>(jsonText).frames.values.map {
            Pair(it.frame, it.duration)
        }
    }

    // 2. sprite sheet bitmap 로드
    val fullBitmap = remember {
        context.assets.open(spriteSheetName).use {
            BitmapFactory.decodeStream(it)
        }
    }

    // 3. 애니메이션 인덱스 관리
    var frameIndex by remember { mutableStateOf(0) }

    // 4. 주기적 프레임 변경
    LaunchedEffect(Unit) {
        while (true) {
            delay(frames[frameIndex].second.toLong())
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    // 5. 현재 프레임 추출 (128x128 고정)
    val (pos, _) = frames[frameIndex]
    val croppedBitmap = remember(frameIndex) {
        Bitmap.createBitmap(fullBitmap, pos.x, pos.y, 128, 128).asImageBitmap()
    }

    Image(
        bitmap = croppedBitmap,
        contentDescription = null,
        modifier = modifier
    )
}