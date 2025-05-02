package com.example.eggi.calendar.data.model

import com.example.eggi.calendar.data.local.Bansoogi
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.BsonObjectId.Companion.invoke
import org.mongodb.kbson.ObjectId

data class DetailReport (
    val date: String,

    val finalEnergyPoint: Int,

    val bansoogiTitle: String,
    val bansoogiResource: Int,

    val standupCount: Int,
    val stretchCount: Int,
    val phoneOffCount: Int,

    val lyingTime: Int,
    val sittingTime: Int,
    val phoneTime: Int,
    val sleepTime: Int?,

    val walkCount: Int,
    val runTime: Int,
    val exerciseTime: Int,
    val stairsClimbed: Int,

    val breakfast: Boolean?,
    val lunch: Boolean?,
    val dinner: Boolean?
)