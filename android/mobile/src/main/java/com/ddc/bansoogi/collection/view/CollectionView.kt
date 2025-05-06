package com.ddc.bansoogi.collection.view

import com.ddc.bansoogi.collection.data.model.CollectionDto

interface CollectionView {
    fun displayCollectionList(collectionDtoList: List<CollectionDto>)
    fun showCharacterDetail(character: CollectionDto)
    fun dismissCharacterDetail()
}