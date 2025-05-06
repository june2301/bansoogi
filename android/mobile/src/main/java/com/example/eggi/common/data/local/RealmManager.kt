//package com.example.eggi.common.data.local
//
//import com.example.eggi.person.data.entity.Person
//import com.example.eggi.myInfo.data.entity.User
//import com.ddc.bansoogi.collection.data.entity.Character
//import com.ddc.bansoogi.collection.data.entity.UnlockedCharacter
//import io.realm.kotlin.Realm
//import io.realm.kotlin.RealmConfiguration
//
//object RealmManager {
//    private val config = RealmConfiguration.create(
//        schema = setOf(
//            Person::class,
//            User::class,
//            Character::class,
//            UnlockedCharacter::class
//        )
//    )
//
//    val realm: Realm by lazy {
//        Realm.open(config)
//    }
//}