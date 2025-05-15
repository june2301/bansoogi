package com.ddc.bansoogi.tile.layout.build

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.ARC_ANCHOR_START

fun buildBackgroundBar(): LayoutElementBuilders.LayoutElement {
    return buildCircleBar(1f, Color.DarkGray)
}

fun buildProgressBar(progress: Float): LayoutElementBuilders.LayoutElement {
    return buildCircleBar(progress, Color(0xFF44C7E3))
}

private fun buildCircleBar(progress: Float, color: Color): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Arc.Builder()
        .addContent(
            LayoutElementBuilders.ArcLine.Builder()
                .setLength(DimensionBuilders.degrees(270f * progress))
                .setThickness(DimensionBuilders.dp(8f))
                .setColor(ColorBuilders.argb(color.toArgb()))
                .build()
        )
        .setAnchorAngle(DimensionBuilders.degrees(225f))
        .setAnchorType(ARC_ANCHOR_START)
        .build()
}