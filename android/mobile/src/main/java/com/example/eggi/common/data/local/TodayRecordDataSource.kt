package com.example.eggi.common.data.local

import com.example.eggi.common.data.entity.TodayRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import io.realm.kotlin.ext.query
import com.example.eggi.R
import io.realm.kotlin.types.RealmInstant
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.BsonObjectId.Companion.invoke
import org.mongodb.kbson.ObjectId

class TodayRecordDataSource {
    private val realm = RealmManager.realm

    fun getTodayRecord(): Flow<TodayRecord> {
        return realm.query<TodayRecord>()
            .asFlow()
            .map { it.list.firstOrNull() ?: TodayRecord() }
    }

    suspend fun initialize() {
        val hasTodayRecord = realm.query<TodayRecord>().find().isNotEmpty()
        if (!hasTodayRecord) {
            realm.write {
                copyToRealm(TodayRecord().apply {
                    energyPoint = 0
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

}
