package com.example.eggi.collection.data.local

import com.example.eggi.collection.data.entity.Character
import com.example.eggi.collection.data.entity.UnlockedCharacter
import com.example.eggi.common.data.local.RealmManager
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionDataSource {
    private val realm = RealmManager.realm

    fun getAllBansoogi(): Flow<List<Character>> =
        realm.query<Character>()
            .asFlow()
            .map { it.list }

    fun getUnlockedBansoogi(): Flow<List<UnlockedCharacter>> =
        realm.query<UnlockedCharacter>()
            .asFlow()
            .map { it.list }
}