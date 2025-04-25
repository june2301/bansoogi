package com.example.eggi.person.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.eggi.R
import com.example.eggi.person.controller.PersonController
import com.example.eggi.person.data.model.Person
import com.example.eggi.person.view.PersonView

class PersonActivity : AppCompatActivity(), PersonView {

    private lateinit var peopleTextView: TextView

    private lateinit var controller: PersonController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person)

        peopleTextView = findViewById(R.id.textViewPeople)

        controller = PersonController(this)
        controller.initialize()

    }

    override fun displayPeople(people: List<Person>) {
        val peopleText = people.joinToString("\n\n") {
            "이름: ${it.name}\nID: ${it.id}"
        }
        peopleTextView.text = peopleText
    }
}