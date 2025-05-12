package com.ddc.bansoogi.myinfo.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.myInfoDataStore: DataStore<Preferences> by preferencesDataStore("my_info")

object MyInfoPreferenceKeys {
    val MYINFO = stringPreferencesKey("my_info")
}