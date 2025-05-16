package com.ddc.bansoogi.common.data.model

import io.realm.kotlin.types.RealmInstant
import org.mongodb.kbson.ObjectId
import java.time.LocalDate

data class TodayRecordDto(
    var recordId: ObjectId,
    var energyPoint: Int,
    var standUpCnt: Int,
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
    var interactionLatestTime: RealmInstant?,
    var isViewed: Boolean,
    var isClosed: Boolean,
    var createdAt: RealmInstant,
    var updatedAt: RealmInstant
)