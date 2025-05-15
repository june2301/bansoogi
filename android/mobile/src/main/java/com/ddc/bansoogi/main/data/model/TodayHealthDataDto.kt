package com.ddc.bansoogi.main.data.model

import org.mongodb.kbson.ObjectId

data class TodayHealthDataDto(
    val id: ObjectId,
    val stepGoal: Int?,
    val steps: Int?,
    val floorsClimbed: Int?,
    val sleepTime: Int?,
    val exerciseTime: Int?,
    val recordedDate: String
)
