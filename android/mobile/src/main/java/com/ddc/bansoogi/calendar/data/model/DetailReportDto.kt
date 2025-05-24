package com.ddc.bansoogi.calendar.data.model

import com.ddc.bansoogi.common.data.model.ActivityLogDto

data class DetailReportDto (
    val date: String,

    val finalEnergyPoint: Int,

    val bansoogiTitle: String,
    val bansoogiGifUrl: String,
    val bansoogiImageUrl: String,

    val standupCount: Int,
    val standLog: List<ActivityLogDto>,

    val stretchCount: Int,
    val stretchLog: List<ActivityLogDto>,

    val phoneOffCount: Int,
    val phoneOffLog: List<ActivityLogDto>,

    val lyingTime: Int,
    val sittingTime: Int,
    val phoneTime: Int,

    val walkCount: Int,
    val stairsClimbed: Int,
    val sleepTime: Int?,
    val exerciseTime: Int,

    val breakfast: Boolean?,
    val lunch: Boolean?,
    val dinner: Boolean?
)