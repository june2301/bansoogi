package com.ddc.bansoogi.collection.data.model

import com.ddc.bansoogi.collection.data.local.CollectionDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CollectionModel {
    private val dataSource = CollectionDataSource()

    fun getCollectionList(): Flow<List<CollectionDto>> =
        combine(
            dataSource.getAllBansoogi(),
            dataSource.getUnlockedBansoogi()
        ) { bansoogiList, unlockedList ->
            bansoogiList.map { entity ->
                CollectionDto(
                    id = entity.bansoogiId,
                    title = entity.title,
                    imageUrl = entity.imageUrl,
                    silhouetteImageUrl = entity.silhouetteImageUrl,
                    gifUrl = entity.gifUrl,
                    description = entity.description,
                    isUnlocked = unlockedList.any { it.bansoogiId == entity.bansoogiId }
                )
            }
        }
}