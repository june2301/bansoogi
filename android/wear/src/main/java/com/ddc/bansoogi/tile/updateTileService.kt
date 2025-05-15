package com.ddc.bansoogi.tile

import android.content.Context
import androidx.wear.tiles.TileService

fun updateTileService(context: Context) {
    TileService.getUpdater(context).requestUpdate(MainTileService::class.java)
}