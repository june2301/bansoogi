package com.ddc.bansoogi.collection.data.local

import android.content.Context
import com.ddc.bansoogi.calendar.data.entity.RecordedReport
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

    fun getBansoogiById(bansoogiId: Int): Character {
        return realm.query<Character>("bansoogiId == $0", bansoogiId)
                .first()
                .find() ?: Character().apply {
                title = "반숙이"
                imageUrl = "bansoogi_default_profile"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi_basic"
                description = "우리의 반숙이입니다."
        }
    }

    fun getImageResourceIdForBansoogiId(context: Context, bansoogiId: Int): Int {
        val imageUrl = realm.query<Character>("bansoogiId == $0", bansoogiId)
            .first()
            .find()
            ?.imageUrl ?: "bansoogi_default_profile"

        return context.resources.getIdentifier(imageUrl, "drawable", context.packageName)
    }

    suspend fun insertDummyCharactersWithUnlock() {
        val alreadyExists = realm.query<Character>().find().isNotEmpty()
        if (alreadyExists) return

        val characterList = listOf(
            Character().apply {
                bansoogiId = 1
                title = "반숙이"
                imageUrl = "bansoogi_default_profile"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi_basic"
                description = "우리의 반숙이입니다."
            },
            Character().apply {
                bansoogiId = 2
                title = "완숙이"
                imageUrl = "bansoogi2_dry"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi2_drying"
                description = "어쩐지 다 익어버렸습니다."
            },
            Character().apply {
                bansoogiId = 3
                title = "흰숙이"
                imageUrl = "bansoogi3_yogurt"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi3_yogurting"
                description = "유난히 하얀 반숙이입니다."
            },
            Character().apply {
                bansoogiId = 4
                title = "엄숙이"
                imageUrl = "bansoogi4_solemn"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi4_solemning"
                description = "다들 조용."
            },
            Character().apply {
                bansoogiId = 5
                title = "뒷태 반숙이"
                imageUrl = "bansoogi5_back"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi5_backing"
                description = "토실토실 반숙이입니다."
            },
            Character().apply {
                bansoogiId = 6
                title = "간장 반숙이"
                imageUrl = "bansoogi6_soysauce"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi6_soysaucing"
                description = "짜지 않게 조금만 뿌렸습니다."
            },
            Character().apply {
                bansoogiId = 7
                title = "날치알 반숙이"
                imageUrl = "bansoogi7_flying_fish"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi7_flying_fishing"
                description = "날치알이 터지지 않게 조심하고 있습니다."
            },
            Character().apply {
                bansoogiId = 8
                title = "후리가케 반숙이"
                imageUrl = "bansoogi8_furikake"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi8_furikaking"
                description = "깨가 튀어서 주근깨가 생겼습니다."
            },
            Character().apply {
                bansoogiId = 9
                title = "반반숙이"
                imageUrl = "bansoogi9_half"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi9_halfing"
                description = "본인이 진짜 반숙이라고 주장하는 중."
            },
            Character().apply {
                bansoogiId = 10
                title = "마늘 반숙이"
                imageUrl = "bansoogi10_garlic"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi10_garlicing"
                description = "마늘을 사랑합니다."
            },
            Character().apply {
                bansoogiId = 11
                title = "껍질 반숙이"
                imageUrl = "bansoogi11_eggshell"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi11_eggshelling"
                description = "나름 힙할지도."
            },
            Character().apply {
                bansoogiId = 12
                title = "헤드셋 반숙이"
                imageUrl = "bansoogi12_headphone"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi12_headphoning"
                description = "요즘 유행하는 햄쪽파 에디션입니다."
            },
            Character().apply {
                bansoogiId = 13
                title = "황금 반숙이"
                imageUrl = "bansoogi13_gold"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi13_golding"
                description = "요란하게 비싼 반숙이입니다."
            },
            Character().apply {
                bansoogiId = 14
                title = "반숙사유상"
                imageUrl = "bansoogi14_pensive_bodhisattva"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi14_pensive_bodhisattving"
                description = "열반에 오른 반숙이입니다."
            },
            Character().apply {
                bansoogiId = 15
                title = "김반숙"
                imageUrl = "bansoogi15_seaweed"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi15_seaweeding"
                description = "짭짤한 헤어스타일의 반숙이입니다."
            },
            Character().apply {
                bansoogiId = 16
                title = "피단 반숙이"
                imageUrl = "bansoogi16_hero"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi16_heroing"
                description = "안 썩었습니다."
            },
            Character().apply {
                bansoogiId = 31
                title = "구름 반숙이"
                imageUrl = "bansoogi31_cloud"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi31_clouding"
                description = "구름 후라이는 푹신하군요."
            },
            Character().apply {
                bansoogiId = 32
                title = "짜계치 반숙이"
                imageUrl = "bansoogi32_zzaechi"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi32_zzaeching"
                description = "풍미가 넘치는 반숙이입니다."
            },
            Character().apply {
                bansoogiId = 33
                title = "전기 반숙이"
                imageUrl = "bansoogi33_pikachu"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi33_pikachuing"
                description = "무언가 닮은 것 같기도."
            },
            Character().apply {
                bansoogiId = 34
                title = "부활절 반숙이"
                imageUrl = "bansoogi34_revive"
                silhouetteImageUrl = "unknown"
                gifUrl = "bansoogi34_reviving"
                description = "마음에 드는 껍질을 써봤습니다."
            }
        )

        realm.write {
            characterList.forEach { copyToRealm(it) }
        }

//        dummyData()
    }

    suspend fun saveUnlockedCharacter(character: Character) {
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

    suspend fun dummyData() {
        if (true) {
            realm.write {
                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 4
                    acquisitionCount = 1
                    createdAt = toRealmInstant("2025-05-17 00:00:00")
                    updatedAt = toRealmInstant("2025-05-17 00:00:00")
                })

                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 5
                    acquisitionCount = 1
                    createdAt = toRealmInstant("2025-05-07 00:00:00")
                    updatedAt = toRealmInstant("2025-05-07 00:00:00")
                })

                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 7
                    acquisitionCount = 3
                    createdAt = toRealmInstant("2025-05-09 00:00:00")
                    updatedAt = toRealmInstant("2025-05-25 00:00:00")
                })

                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 8
                    acquisitionCount = 1
                    createdAt = toRealmInstant("2025-05-10 00:00:00")
                    updatedAt = toRealmInstant("2025-05-10 00:00:00")
                })

                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 10
                    acquisitionCount = 1
                    createdAt = toRealmInstant("2025-05-22 00:00:00")
                    updatedAt = toRealmInstant("2025-05-22 00:00:00")
                })


                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 11
                    acquisitionCount = 2
                    createdAt = toRealmInstant("2025-05-02 00:00:00")
                    updatedAt = toRealmInstant("2025-05-11 00:00:00")
                })

                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 12
                    acquisitionCount = 2
                    createdAt = toRealmInstant("2025-05-12 00:00:00")
                    updatedAt = toRealmInstant("2025-05-14 00:00:00")
                })

                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 13
                    acquisitionCount = 2
                    createdAt = toRealmInstant("2025-05-03 00:00:00")
                    updatedAt = toRealmInstant("2025-05-19 00:00:00")
                })

                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 15
                    acquisitionCount = 2
                    createdAt = toRealmInstant("2025-05-15 00:00:00")
                    updatedAt = toRealmInstant("2025-05-20 00:00:00")
                })

                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 31
                    acquisitionCount = 2
                    createdAt = toRealmInstant("2025-05-04 00:00:00")
                    updatedAt = toRealmInstant("2025-05-08 00:00:00")
                })

                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 32
                    acquisitionCount = 2
                    createdAt = toRealmInstant("2025-05-21 00:00:00")
                    updatedAt = toRealmInstant("2025-05-05 00:00:00")
                })

                copyToRealm(UnlockedCharacter().apply {
                    bansoogiId = 33
                    acquisitionCount = 2
                    createdAt = toRealmInstant("2025-05-06 00:00:00")
                    updatedAt = toRealmInstant("2025-05-26 00:00:00")
                })
            }
        }
    }

    // RealmInstant 변환 헬퍼 함수
    private fun toRealmInstant(dateTimeString: String): RealmInstant {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val date = format.parse(dateTimeString)
        val epochSeconds = date.time / 1000
        return RealmInstant.from(epochSeconds, 0)
    }
}