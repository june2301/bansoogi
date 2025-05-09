package com.ddc.bansoogi.collection.util

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import com.ddc.bansoogi.R
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

fun saveDownloadableImage(
    activity: ComponentActivity,
    characterBitmap: Bitmap,
    title: String,
    description: String
) {
    val width = 446
    val height = 669
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    // 배경 그리기
    val background = androidx.core.content.ContextCompat.getDrawable(activity, R.drawable.background)
    background?.setBounds(0, 0, width, height)
    background?.draw(canvas)

    // 캐릭터 이미지 그리기 (중앙 상단)
    val characterSize = 400
    val left = (width - characterSize) / 2
    val top = 100
    val resizedCharacter = characterBitmap.scale(characterSize, characterSize)
    canvas.drawBitmap(resizedCharacter, left.toFloat(), top.toFloat(), null)

    // 텍스트 스타일 설정
    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 32f
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val descPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 24f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }

    // 텍스트 그리기
    val titleY = height - 130f
    val descY = height - 80f
    canvas.drawText(title, width / 2f, titleY, titlePaint)
    canvas.drawText(description, width / 2f, descY, descPaint)

    // 파일명 생성 및 저장
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val filename = "bansoogi_$timestamp"
    val success = saveBitmapToGallery(activity, bitmap, filename)

    android.widget.Toast.makeText(
        activity,
        if (success) "이미지가 저장되었습니다" else "저장에 실패했습니다",
        android.widget.Toast.LENGTH_SHORT
    ).show()
}