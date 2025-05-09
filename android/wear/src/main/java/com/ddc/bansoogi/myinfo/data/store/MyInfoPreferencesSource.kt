package com.ddc.bansoogi.myinfo.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.ddc.bansoogi.myinfo.data.dto.MyInfoDto
import com.ddc.bansoogi.myinfo.data.mapper.MyInfoJsonMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

suspend fun saveMyInfoCache(context: Context, myInfo: MyInfoDto) {
    val json = MyInfoJsonMapper.toJson(myInfo)

    context.myInfoDataStore.edit { prefs ->
        prefs[MyInfoPreferenceKeys.MYINFO] = json
    }
}

fun getCachedMyInfo(context: Context): Flow<MyInfoDto?> {
    return context.myInfoDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val json = prefs[MyInfoPreferenceKeys.MYINFO] ?: ""

            runCatching {
                MyInfoJsonMapper.fromJson(json)
            }.getOrNull()
        }
}