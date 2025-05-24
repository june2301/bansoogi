package com.ddc.bansoogi.tile.layout.build

import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders

fun buildBansoogiImage(resource: String): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Image.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
        .setResourceId(resource)
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
}