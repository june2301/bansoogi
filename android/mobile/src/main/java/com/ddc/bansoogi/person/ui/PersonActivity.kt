package com.ddc.bansoogi.person.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddc.bansoogi.person.controller.PersonController
import com.ddc.bansoogi.person.data.model.Person
import com.ddc.bansoogi.person.view.PersonView

class PersonActivity : ComponentActivity(), PersonView {

    private lateinit var controller: PersonController

    private val peopleState = mutableStateOf<List<Person>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controller = PersonController(this)

        setContent {
            PersonScreen(people = peopleState.value)
        }

        controller.initialize()
    }

    override fun displayPeople(people: List<Person>) {
        peopleState.value = people
    }
}

@Composable
fun PersonScreen(people: List<Person>) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (people.isEmpty()) {
                Text(
                    text = "준비 중입니다.",
                    fontSize = 18.sp
                )
            } else {
                people.forEach { person ->
                    PersonItem(person = person)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun PersonItem(person: Person) {
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = "이름: ${person.name}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ID: ${person.id}",
            fontSize = 14.sp
        )
    }
}