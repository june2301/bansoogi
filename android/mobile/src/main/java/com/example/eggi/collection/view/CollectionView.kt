package com.example.eggi.collection.view

import com.example.eggi.collection.data.model.CollectionDto

interface CollectionView {
    fun displayCollectionList(collectionDtoList: List<CollectionDto>)
    fun showCharacterDetail(character: CollectionDto)
    fun dismissCharacterDetail()
}