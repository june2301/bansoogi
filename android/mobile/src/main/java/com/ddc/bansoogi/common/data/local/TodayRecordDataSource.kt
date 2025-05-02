package com.ddc.bansoogi.common.data.local

import com.ddc.bansoogi.common.data.entity.TodayRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmInstant
import org.mongodb.kbson.ObjectId

class TodayRecordDataSource {
    private val realm = RealmManager.realm

    suspend fun initialize() {
        val hasTodayRecord = realm.query<TodayRecord>().find().isNotEmpty()
        if (!hasTodayRecord) {
            realm.write {
                copyToRealm(TodayRecord().apply {
                    energyPoint = 0
                    standUpCnt = 0
                    stretchCnt = 0
                    phoneOffCnt = 0
                    lyingTime = 0
                    sittingTime = 0
                    phoneTime = 0
                    sleepTime = 0
                    breakfast = false
                    lunch = false
                    dinner = false
                    interactionCnt = 0
                    isClosed = false
                    createdAt = RealmInstant.now()
                    updatedAt = RealmInstant.now()
                })
            }
        }
    }

    fun getTodayRecord(): Flow<TodayRecord?> {
        return realm.query<TodayRecord>()
            .asFlow()
            .map { it.list.firstOrNull() }
    }

    suspend fun updateInteractionCnt(recordId: ObjectId) {
        realm.write {
            query<TodayRecord>("recordId == $0", recordId)
                .first()
                .find()
                ?.let { record ->
                    findLatest(record)?.apply {
                        interactionCnt += 1

                        updatedAt = RealmInstant.now()
                    }
                }
        }
    }

    suspend fun updateEnergy(recordId: ObjectId, addedEnergy: Int) {
        realm.write {
            query<TodayRecord>("recordId == $0", recordId)
                .first()
                .find()
                ?.let { record ->
                    findLatest(record)?.apply {
                        energyPoint = minOf(energyPoint + addedEnergy, 100)
                    }
                }
        }
    }
}
