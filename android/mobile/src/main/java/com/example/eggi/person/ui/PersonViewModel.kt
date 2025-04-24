package com.example.eggi.person.ui

import androidx.lifecycle.ViewModel
import com.example.eggi.person.data.local.PersonDataSource
import com.example.eggi.person.data.repository.PersonRepository

class PersonViewModel: ViewModel() {
    private val repository = PersonRepository(PersonDataSource())

    val people = repository.getPeople()
}