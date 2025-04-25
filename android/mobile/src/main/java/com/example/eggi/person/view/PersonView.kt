package com.example.eggi.person.view

import com.example.eggi.person.data.model.Person

interface PersonView {
    fun displayPeople(people: List<Person>)
}