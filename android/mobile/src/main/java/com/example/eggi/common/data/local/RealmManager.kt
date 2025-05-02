package com.example.eggi.common.data.local

import com.example.eggi.calendar.data.entity.RecordedReport
import com.example.eggi.calendar.data.local.Bansoogi
import com.example.eggi.person.data.entity.Person
import com.example.eggi.myInfo.data.entity.User
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

object RealmManager {
    private val config = RealmConfiguration.create(
        schema = setOf(
            Person::class,
            User::class,
            Bansoogi::class,
            RecordedReport::class,
        )
    )

    val realm: Realm by lazy {
        Realm.open(config)
    }
}