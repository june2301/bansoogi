package com.ddc.bansoogi.collection.controller

import com.ddc.bansoogi.collection.data.model.CollectionDto
import com.ddc.bansoogi.collection.data.model.CollectionModel
import com.ddc.bansoogi.collection.view.CollectionView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CollectionController(private val view: CollectionView) {
    private val model = CollectionModel()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        scope.launch {
            model.getCollectionList().collectLatest { collectionList ->
                view.displayCollectionList(collectionList)
            }
        }
    }

    fun onCharacterClick(character: CollectionDto) {
        view.showCharacterDetail(character)
    }

    fun dismissCharacterDetail() {
        view.dismissCharacterDetail()
    }
}