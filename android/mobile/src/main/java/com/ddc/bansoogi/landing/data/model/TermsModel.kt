package com.ddc.bansoogi.landing.data.model

class TermsModel {
    var serviceChecked: Boolean = false
    var privacyChecked: Boolean = false
    var healthChecked: Boolean = false

    fun isAllAgreed(): Boolean {
        return serviceChecked && privacyChecked && healthChecked
    }

    fun reset() {
        serviceChecked = false
        privacyChecked = false
        healthChecked = false
    }
}