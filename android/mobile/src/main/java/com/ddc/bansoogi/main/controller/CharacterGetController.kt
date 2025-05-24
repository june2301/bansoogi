package com.ddc.bansoogi.main.controller

import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.common.data.model.TodayRecordModel
import com.ddc.bansoogi.main.data.model.CharacterGetModel
import kotlinx.coroutines.flow.Flow

class CharacterGetController {
    private val model = CharacterGetModel()
    private val todayRecordModel = TodayRecordModel()

    fun canDrawCharacter(): Boolean {
        val today = todayRecordModel.getTodayRecordSync()
        return (today?.energyPoint ?: 0) >= 80
    }

    fun getRandomBansoogi(): Flow<Character?> {
        return model.getRandomBansoogi()
    }

    suspend fun saveUnlockedCharacter(character: Character) {
        model.saveUnlockedCharacter(character)
    }
}