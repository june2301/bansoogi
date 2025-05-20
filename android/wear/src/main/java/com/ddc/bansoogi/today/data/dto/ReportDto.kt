package com.ddc.bansoogi.today.data.dto

data class ReportDto (
    val energyPoint: Int,

    val standupCount: Int,
    val stretchCount: Int,
    val phoneOffCount: Int,

    val lyingTime: Int,
    val sittingTime: Int,
    val phoneTime: Int,

    val walkCount: Int,
    val stairsClimbed: Int,
    val sleepTime: Int,
    val exerciseTime: Int,

    val breakfast: Boolean?,
    val lunch: Boolean?,
    val dinner: Boolean?
) {
    companion object {
        fun default() = ReportDto(
            energyPoint = 0,

            standupCount = 0,
            stretchCount = 0,
            phoneOffCount = 0,

            lyingTime = 0,
            sittingTime = 0,
            phoneTime = 0,

            walkCount = 0,
            stairsClimbed = 0,
            sleepTime = 0,
            exerciseTime = 0,

            breakfast = false,
            lunch = false,
            dinner = false
        )
    }
}