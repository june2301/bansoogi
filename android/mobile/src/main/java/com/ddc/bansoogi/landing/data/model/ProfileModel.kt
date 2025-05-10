package com.ddc.bansoogi.landing.data.model

import java.util.Date

class ProfileModel {
    var nickname: String = ""
    var birthDate: Date = Date()

    fun isValid(): Boolean {
        return nickname.isNotBlank()
    }

    fun reset() {
        nickname = ""
        birthDate = Date()
    }
}