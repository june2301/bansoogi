package com.ddc.bansoogi.tile

import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.ddc.bansoogi.tile.layout.TileLayout
import com.ddc.bansoogi.tile.resource.TileResources
import com.ddc.bansoogi.today.data.store.getCachedReport
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import kotlinx.coroutines.flow.firstOrNull

@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = TileResources.resources(requestParams)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val energyPoint = getEnergyPoint()

        return TileLayout.tile(requestParams, this, energyPoint)
    }

    private suspend fun getEnergyPoint(): Int {
        return try {
            val reportDto = getCachedReport(this).firstOrNull()
            reportDto?.energyPoint ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0 // 오류 발생 시 기본값 0 반환
        }
    }
}
