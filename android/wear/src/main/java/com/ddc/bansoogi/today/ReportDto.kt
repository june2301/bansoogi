package com.ddc.bansoogi.today

data class ReportDto (
    val energyPoint: Int,

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