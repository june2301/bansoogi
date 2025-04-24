package com.example.eggi.person.data.local

import com.example.eggi.common.data.local.RealmManager
import com.example.eggi.person.data.entity.Person
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId

class PersonDataSource {
    private val realm = RealmManager.realm

    suspend fun insertPerson(name: String): ObjectId {
        val id = BsonObjectId()
        realm.write {
            copyToRealm(Person().apply {
                _id = id
                this.name = name
            })
        }
        return id
    }

    fun getAllPeople(): Flow<List<Person>> {
        return realm.query<Person>().asFlow().map {it.list}
    }
}