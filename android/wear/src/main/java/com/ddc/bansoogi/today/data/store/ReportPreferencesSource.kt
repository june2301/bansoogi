package com.ddc.bansoogi.today.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.ddc.bansoogi.today.data.dto.ReportDto
import com.ddc.bansoogi.today.data.mapper.ReportJsonMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

suspend fun saveReportCache(context: Context, report: ReportDto) {
    val json = ReportJsonMapper.toJson(report)

    context.reportDataStore.edit { prefs ->
        prefs[ReportPreferenceKeys.RECORD] = json
    }
}

fun getCachedReport(context: Context): Flow<ReportDto?> {
    return context.reportDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val json = prefs[ReportPreferenceKeys.RECORD] ?: ""

            runCatching {
                ReportJsonMapper.fromJson(json)
            }.getOrNull()
        }
}