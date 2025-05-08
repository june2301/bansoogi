package com.ddc.bansoogi.collection.data.local

import android.content.Context
import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.collection.data.entity.UnlockedCharacter
import com.ddc.bansoogi.common.data.local.RealmManager
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmInstant
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

    fun getImageResourceIdForBansoogiId(context: Context, bansoogiId: Int): Int {
        val realm = RealmManager.realm
        val imageUrl = realm.query<Character>("bansoogiId == $0", bansoogiId)
            .first()
            .find()
            ?.imageUrl ?: "bansoogi_default_profile"

        return context.resources.getIdentifier(imageUrl, "drawable", context.packageName)
    }

    suspend fun insertDummyCharactersWithUnlock() {
        val realm = RealmManager.realm

        val alreadyExists = realm.query<Character>().find().isNotEmpty()
        if (alreadyExists) return

        val characterList = listOf(
            Character().apply {
                bansoogiId = 1
                title = "기본 반숙"
                imageUrl = "bansoogi_default_profile"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi_basic"
                description = "가장 처음 등장하는 반숙이입니다."
            },
            Character().apply {
                bansoogiId = 2
                title = "밥먹는 반숙"
                imageUrl = "bansoogi_eat"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi_eating"
                description = "밥먹는 반숙이입니다."
            }
        )

        val unlockList = listOf(
            UnlockedCharacter().apply {
                bansoogiId = 2 // bansoogiId = 2 캐릭터만 해금된 상태
                acquisitionCount = 1
                createdAt = RealmInstant.now()
                updatedAt = RealmInstant.now()
            }
        )

        realm.write {
            characterList.forEach { copyToRealm(it) }
            unlockList.forEach { copyToRealm(it) }
        }
    }
}