package com.ddc.bansoogi.person.view

import com.ddc.bansoogi.person.data.model.Person

interface PersonView {
    fun displayPeople(people: List<Person>)
}