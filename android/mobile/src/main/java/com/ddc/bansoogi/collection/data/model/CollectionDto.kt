package com.ddc.bansoogi.collection.data.model

data class CollectionDto(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val silhouetteImageUrl: String,
    val gifUrl: String,
    val description: String,
    val isUnlocked: Boolean
)
