package com.ddc.bansoogi.today.data.mapper

import com.ddc.bansoogi.today.data.dto.ReportDto
import com.google.gson.Gson

object ReportJsonMapper {
    fun toJson(report: ReportDto): String {
        return Gson().toJson(report)
    }

    fun fromJson(json: String): ReportDto {
        return Gson().fromJson(json, ReportDto::class.java)
    }
}