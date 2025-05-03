package com.ddc.bansoogi.person.data.model

import com.ddc.bansoogi.person.data.local.PersonDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PersonModel {
    private val dataSource = PersonDataSource()

    suspend fun addPerson(name: String): String {
        return dataSource.insertPerson(name).toString()
    }

    fun getPeople(): Flow<List<Person>> {
        return dataSource.getAllPeople().map { entities ->
            entities.map { entity ->
                Person(
                    id = entity._id.toString(),
                    name = entity.name
                )
            }
        }
    }
}