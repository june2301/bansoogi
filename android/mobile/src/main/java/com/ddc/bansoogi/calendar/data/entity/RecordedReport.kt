package com.ddc.bansoogi.calendar.data.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId

class RecordedReport : RealmObject {
    @PrimaryKey
    var reportId: ObjectId = BsonObjectId.Companion()

    var finalEnergyPoint: Int = 0
    var bansoogiId: Int = 0

    var standupCount: Int = 0
    var stretchCount: Int = 0
    var phoneOffCount: Int = 0

    var lyingTime: Int = 0
    var sittingTime: Int = 0
    var phoneTime: Int = 0

    var walkCount: Int = 0
    var stairsClimbed: Int = 0
    var sleepTime: Int? = 0
    var exerciseTime: Int = 0

    var breakfast: Boolean? = false
    var lunch: Boolean? = false
    var dinner: Boolean? = false

    var reportedDate: String = ""
}