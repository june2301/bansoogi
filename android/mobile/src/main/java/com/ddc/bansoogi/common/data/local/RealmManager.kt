package com.ddc.bansoogi.common.data.local

import com.ddc.bansoogi.calendar.data.entity.RecordedReport
import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.collection.data.entity.UnlockedCharacter
import com.ddc.bansoogi.common.data.entity.ActivityLog
import com.ddc.bansoogi.common.data.entity.TodayRecord
import com.ddc.bansoogi.main.data.entity.TodayHealthData
import com.ddc.bansoogi.person.data.entity.Person
import com.ddc.bansoogi.myInfo.data.entity.User
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import java.io.File

object RealmManager {
    private val config = RealmConfiguration.create(
        schema = setOf(
            Person::class,
            User::class,
            RecordedReport::class,
            TodayRecord::class,
            Character::class,
            UnlockedCharacter::class,
            ActivityLog::class,
            TodayHealthData::class,
        )
    )

    @Volatile
    private var _realm: Realm? = null

    val realm: Realm
        get() = _realm ?: synchronized(this) {
            _realm ?: Realm.open(config).also { _realm = it }
        }

    @Synchronized
    fun clearAll() {
        realm.writeBlocking { deleteAll() }
    }
}