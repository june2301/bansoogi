package com.ddc.bansoogi.common.wear.data.model

data class WearReportDto (
    val energyPoint: Int,

    val standupCount: Int,
    val stretchCount: Int,
    val phoneOffCount: Int,

    val lyingTime: Int,
    val sittingTime: Int,
    val phoneTime: Int,

    val walkCount: Long,
    val stairsClimbed: Float,
    val sleepTime: Int?,
    val exerciseTime: Int,

    val breakfast: Boolean?,
    val lunch: Boolean?,
    val dinner: Boolean?
)