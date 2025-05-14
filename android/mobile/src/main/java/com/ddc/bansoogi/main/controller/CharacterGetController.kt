package com.ddc.bansoogi.main.controller

import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.collection.data.entity.UnlockedCharacter
import com.ddc.bansoogi.common.data.entity.TodayRecord
import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.collection.util.CharacterPicker
import com.ddc.bansoogi.main.model.CharacterGetModel
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmInstant
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
        val realm = RealmManager.realm
        val bansoogiId = character.bansoogiId
        realm.write {
            val existing = query<UnlockedCharacter>("bansoogiId == $0", bansoogiId).first().find()
            if (existing != null) {
                findLatest(existing)?.apply {
                    acquisitionCount += 1
                    updatedAt = RealmInstant.now()
                }
            } else {
                copyToRealm(
                    UnlockedCharacter().apply {
                        this.bansoogiId = bansoogiId
                        this.acquisitionCount = 1
                        this.createdAt = RealmInstant.now()
                        this.updatedAt = RealmInstant.now()
                    }
                )
            }
        }
    }
}