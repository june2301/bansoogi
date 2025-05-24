package com.ddc.bansoogi.tile.resource

import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.RequestBuilders
import com.ddc.bansoogi.R

object TileResources {
    const val RESOURCES_VERSION = "0"

    fun resources(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping(
                "bansoogi_basic_image",
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.bansoogi_basic)
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
