package com.ddc.bansoogi.main.data.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId

class TodayHealthData : RealmObject {
    @PrimaryKey
    var id: ObjectId = BsonObjectId()

    var stepGoal: Int? = null
    var steps: Int? = null
    var floorsClimbed: Int? = null
    var sleepTime: Int? = null
    var exerciseTime: Int? = null
    var recordedDate: String = ""
}