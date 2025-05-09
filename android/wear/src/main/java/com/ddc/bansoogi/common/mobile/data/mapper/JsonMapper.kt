package com.ddc.bansoogi.common.mobile.data.mapper

import com.google.gson.Gson

object JsonMapper {
    private val gson = Gson()

    fun <T> toJson(obj: T): String {
        return gson.toJson(obj)
    }

    internal inline fun <reified T> fromJson(json: String): T {
        return gson.fromJson(json, T::class.java)
    }
}