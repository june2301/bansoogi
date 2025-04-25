package com.example.eggi.person.controller

import com.example.eggi.person.data.model.PersonModel
import com.example.eggi.person.view.PersonView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PersonController(private val view: PersonView) {
    private val model = PersonModel()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        loadPeople()
    }

    private fun loadPeople() {
        coroutineScope.launch {
            model.getPeople().collectLatest { people ->
                view.displayPeople(people)
            }
        }
    }
}