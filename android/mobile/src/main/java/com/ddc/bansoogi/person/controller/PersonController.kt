package com.ddc.bansoogi.person.controller

import com.ddc.bansoogi.person.data.model.PersonModel
import com.ddc.bansoogi.person.view.PersonView
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