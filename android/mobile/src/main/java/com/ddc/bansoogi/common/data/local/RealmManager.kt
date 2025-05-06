package com.ddc.bansoogi.common.data.local

import com.ddc.bansoogi.calendar.data.entity.RecordedReport
import com.ddc.bansoogi.calendar.data.local.Bansoogi
import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.collection.data.entity.UnlockedCharacter
import com.ddc.bansoogi.common.data.entity.TodayRecord
import com.ddc.bansoogi.person.data.entity.Person
import com.ddc.bansoogi.myInfo.data.entity.User
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

object RealmManager {
    private val config = RealmConfiguration.create(
        schema = setOf(
            Person::class,
            User::class,
            Bansoogi::class,
            RecordedReport::class,
            TodayRecord::class,
            Character::class,
            UnlockedCharacter::class,
        )
    )

    val realm: Realm by lazy {
        Realm.open(config)
    }
}