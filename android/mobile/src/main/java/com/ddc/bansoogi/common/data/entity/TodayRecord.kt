package com.ddc.bansoogi.common.data.entity

import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class TodayRecord() : RealmObject{
    @PrimaryKey
    var recordId: ObjectId = ObjectId()

    var energyPoint: Int = 0
    var standUpCnt: Int = 0
    var stretchCnt: Int = 0
    var phoneOffCnt: Int = 0
    var lyingTime: Int = 0
    var sittingTime: Int = 0
    var phoneTime: Int = 0
    var sleepTime: Int = 0
    var breakfast: Boolean = false
    var lunch: Boolean = false
    var dinner: Boolean = false
    var interactionCnt: Int = 0
    var interactionLatestTime: RealmInstant? = null
    var isClosed: Boolean = false
    var createdAt: RealmInstant = RealmInstant.now()
    var updatedAt: RealmInstant = RealmInstant.now()
}