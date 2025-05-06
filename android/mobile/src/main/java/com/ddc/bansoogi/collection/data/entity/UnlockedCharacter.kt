package com.ddc.bansoogi.collection.data.entity

import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class UnlockedCharacter : RealmObject {
    @PrimaryKey
    var unlockId: ObjectId = ObjectId()

    var bansoogiId: Int = 0
    var acquisitionCount: Int = 1

    var createdAt: RealmInstant = RealmInstant.now()
    var updatedAt: RealmInstant = RealmInstant.now()
}