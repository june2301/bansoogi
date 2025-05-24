package com.ddc.bansoogi.collection.util

import com.ddc.bansoogi.collection.data.entity.Character

object CharacterPicker {
    fun pickRandomBansoogi(all: List<Character>): Character {
        val commons = all.filter { it.bansoogiId <= 30 }
        val hiddens = all.filter { it.bansoogiId > 30 }

        return if ((1..100).random() <= 90 && commons.isNotEmpty()) {
            commons.random()
        } else {
            hiddens.randomOrNull() ?: commons.random()
        }
    }
}