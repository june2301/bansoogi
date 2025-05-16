package com.ddc.bansoogi.collection.data.model

import android.content.Context
import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.collection.data.local.CollectionDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CollectionModel {
    private val dataSource = CollectionDataSource()

    fun getBansoogiById(bansoogiId: Int): Character {
        if (bansoogiId == 0) {
            return Character().apply {
                title = "알"
                imageUrl = "egg_before_broken"
                gifUrl = "egg_before_broken"
                description = "오늘은 반숙이가 집에 갔어요."
            }
        }

        return dataSource.getBansoogiById(bansoogiId)
    }

    fun getImageResId(context: Context, bansoogiId: Int): Int {
        return dataSource.getImageResourceIdForBansoogiId(context, bansoogiId)
    }

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
                    isUnlocked = unlockedList.any { it.bansoogiId == entity.bansoogiId },
                    acquisitionCount = unlockedList.find { it.bansoogiId == entity.bansoogiId }?.acquisitionCount,
                    createdAt = unlockedList.find { it.bansoogiId == entity.bansoogiId }?.createdAt
                )
            }
        }
}