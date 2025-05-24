package com.ddc.bansoogi.tile.layout.build

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography

fun buildEnergyText(context: Context, energy: Int): LayoutElementBuilders.LayoutElement {
    return Box.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
        .addContent(
            Text.Builder(context, "$energy / 100")
                .setColor(argb(Color.White.toArgb()))
                .setTypography(Typography.TYPOGRAPHY_TITLE2)
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setBottom(DimensionBuilders.dp(20f))
                                .build()
                        )
                        .build()
                )
                .build()
        )
        .build()
}