package com.example.eggi.person.ui

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.eggi.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PersonActivity : AppCompatActivity() {

    private lateinit var peopleTextView: TextView

    private val viewModel: PersonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person)

        peopleTextView = findViewById(R.id.textViewPeople)

        lifecycleScope.launch {
            viewModel.people.collectLatest { people ->
                // 간단하게 텍스트뷰에 목록 표시
                val peopleText = people.joinToString("\n\n") {
                    "이름: ${it.name}\nID: ${it.id}"
                }
                peopleTextView.text = peopleText
            }
        }
    }
}