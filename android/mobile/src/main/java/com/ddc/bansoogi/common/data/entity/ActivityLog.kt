package com.ddc.bansoogi.common.data.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId

class ActivityLog : RealmObject {
    @PrimaryKey
    var logId: ObjectId = BsonObjectId()

    var type: String = ""
    var fromState: String = ""
    var duration: Int? = null
    var reactedDate: String = ""
    var reactedTime: String = ""
}