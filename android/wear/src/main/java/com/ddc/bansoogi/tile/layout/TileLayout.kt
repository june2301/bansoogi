package com.ddc.bansoogi.tile.layout

import android.content.Context
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.ddc.bansoogi.tile.layout.build.buildBackgroundBar
import com.ddc.bansoogi.tile.layout.build.buildBansoogiImage
import com.ddc.bansoogi.tile.layout.build.buildEnergyText
import com.ddc.bansoogi.tile.layout.build.buildProgressBar
import com.ddc.bansoogi.tile.resource.TileResourceIds
import com.ddc.bansoogi.tile.resource.TileResources.RESOURCES_VERSION
import com.ddc.bansoogi.tile.layout.build.buildClickableModifier

object TileLayout {
    fun tile(
        requestParams: RequestBuilders.TileRequest,
        context: Context,
        energyPoint: Int
    ): TileBuilders.Tile {
        val singleTileTimeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(tileLayout(requestParams, context, energyPoint))
                            .build()
                    )
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(singleTileTimeline)
            .build()
    }

    private fun tileLayout(
        requestParams: RequestBuilders.TileRequest,
        context: Context,
        energyPoint: Int
    ): LayoutElementBuilders.LayoutElement {
        val progressValue = energyPoint / 100f

        return Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                buildClickableModifier(context) // 타일 클릭
            )
            .addContent(
                buildBackgroundBar()
            )
            .addContent(
                buildProgressBar(progressValue)
            )
            .addContent(
                buildBansoogiImage(TileResourceIds.BASIC)
            )
            .addContent(
                buildEnergyText(context, energyPoint)
            )
            .build()
    }
}