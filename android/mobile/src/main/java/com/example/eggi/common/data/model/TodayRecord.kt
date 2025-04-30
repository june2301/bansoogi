package com.example.eggi.common.data.model

import io.realm.kotlin.types.RealmInstant
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.BsonObjectId.Companion.invoke
import org.mongodb.kbson.ObjectId

data class TodayRecord(
    var recordId: String,
    var energyPoint: Int,
    var stretchCnt: Int,
    var phoneOffCnt: Int,
    var lyingTime: Int,
    var sittingTime: Int,
    var phoneTime: Int,
    var sleepTime: Int,
    var breakfast: Boolean,
    var lunch: Boolean,
    var dinner: Boolean,
    var interactionCnt: Int,
    var isClosed: Boolean,
    var createdAt: RealmInstant,
    var updatedAt: RealmInstant
)