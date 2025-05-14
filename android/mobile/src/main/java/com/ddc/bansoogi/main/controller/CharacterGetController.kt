package com.ddc.bansoogi.main.controller

import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.common.data.entity.TodayRecord
import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.main.data.model.CharacterGetModel
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow

class CharacterGetController {
    private val model = CharacterGetModel()

    fun canDrawCharacter(): Boolean {
        val realm = RealmManager.realm
        val today = realm.query<TodayRecord>().find().lastOrNull()
        return (today?.energyPoint ?: 0) >= 80
    }

    fun getRandomBansoogi(): Flow<Character?> {
        return model.getRandomBansoogi()
    }

    suspend fun saveUnlockedCharacter(character: Character) {
        model.saveUnlockedCharacter(character)
    }
}