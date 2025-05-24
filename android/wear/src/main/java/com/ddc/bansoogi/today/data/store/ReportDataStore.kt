package com.ddc.bansoogi.today.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.reportDataStore: DataStore<Preferences> by preferencesDataStore("today_record_report")

object ReportPreferenceKeys {
    val RECORD = stringPreferencesKey("today_report")
}