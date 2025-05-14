package com.ddc.bansoogi.main.model

import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.collection.data.local.CollectionDataSource
import com.ddc.bansoogi.collection.util.CharacterPicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CharacterGetModel {
    private val dataSource = CollectionDataSource()

    fun getRandomBansoogi(): Flow<Character?> {
        return dataSource.getAllBansoogi().map { list ->
            CharacterPicker.pickRandomBansoogi(list)
        }
    }
}