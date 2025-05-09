package com.ddc.bansoogi.myinfo.data.mapper

import com.ddc.bansoogi.myinfo.data.dto.MyInfoDto
import com.google.gson.Gson

object MyInfoJsonMapper {
    fun toJson(myInfo: MyInfoDto): String {
        return Gson().toJson(myInfo)
    }

    fun fromJson(json: String): MyInfoDto {
        return Gson().fromJson(json, MyInfoDto::class.java)
    }
}