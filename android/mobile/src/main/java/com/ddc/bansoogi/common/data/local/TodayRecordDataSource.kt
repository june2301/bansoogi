package com.ddc.bansoogi.common.data.local

import com.ddc.bansoogi.common.data.enum.MealType
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
                copyToRealm(TodayRecord())
            }
        }
        // TODO: 삭제해야함 (test 용)
//         updateIsClosed()
    }

    suspend fun updateIsClosed() {
        realm.write {
            query<TodayRecord>()
                .first()
                .find()
                ?.let { record ->
                    findLatest(record)?.apply {
                        isClosed = true
                    }
                }
        }
    }

    suspend fun renewTodayRecord() {
        val hasTodayRecord = realm.query<TodayRecord>().find().isNotEmpty()
        if (hasTodayRecord) {
            realm.write {
                var todayRecord = query<TodayRecord>().first().find()
                todayRecord?.let {
                    delete(it)
                }
            }
        }
        realm.write {
            copyToRealm(TodayRecord())
        }
    }

    fun getTodayRecordSync(): TodayRecord? {
        return realm.query<TodayRecord>().first().find()
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

    suspend fun updateInteractionTime(recordId: ObjectId) {
        realm.write {
            query<TodayRecord>("recordId == $0", recordId)
                .first()
                .find()
                ?.let { record ->
                    findLatest(record)?.apply {
                        interactionLatestTime = RealmInstant.now()
                    }
                }
        }
    }

    suspend fun updateIsViewed(recordId: ObjectId, viewed: Boolean) {
        realm.write {
            query<TodayRecord>("recordId == $0", recordId)
                .first()
                .find()
                ?.let { record ->
                    findLatest(record)?.apply {
                        isViewed = viewed
                    }
                }
        }
    }

    suspend fun markMealDone(recordId: ObjectId, mealType: MealType) {
        realm.write {
            query<TodayRecord>("recordId == $0", recordId)
                .first()
                .find()
                ?.apply {
                    when (mealType) {
                        MealType.BREAKFAST -> breakfast = true
                        MealType.LUNCH     -> lunch = true
                        MealType.DINNER    -> dinner = true
                    }
                }
        }
    }

    fun isViewed(): Boolean {
        return realm.query<TodayRecord>()
            .first()
            .find()
            ?.isViewed ?: false
    }
}
