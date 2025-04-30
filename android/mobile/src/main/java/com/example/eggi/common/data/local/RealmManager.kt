package com.example.eggi.common.data.local

import com.example.eggi.common.data.entity.TodayRecord
import com.example.eggi.person.data.entity.Person
import com.example.eggi.myInfo.data.entity.User
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

object RealmManager {
    private val config = RealmConfiguration.create(
        schema = setOf(
            Person::class,
            User::class,
            TodayRecord::class,
        )
    )

    val realm: Realm by lazy {
        Realm.open(config)
    }
}