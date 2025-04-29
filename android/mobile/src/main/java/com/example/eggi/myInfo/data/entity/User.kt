package com.example.eggi.myInfo.data.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId
import com.example.eggi.R

class User : RealmObject {
    @PrimaryKey
    var userId: ObjectId = ObjectId()

    var name: String = ""
    var birthDate: String = ""
    var profileBansoogiId: Int = R.drawable.bansoogi_default_profile

    var wakeUpTime: String = "07:00"
    var sleepTime: String = "22:30"

    var breakfastTime: String = "08:00"
    var lunchTime: String = "12:00"
    var dinnerTime: String = "18:00"

    var notificationDuration: Int = 30

    var alarmEnabled: Boolean = false
    var bgSoundEnabled: Boolean = false
    var effectSoundEnabled: Boolean = false
}
