package com.ddc.bansoogi.landing.controller

import com.ddc.bansoogi.landing.data.model.ProfileModel
import com.ddc.bansoogi.landing.data.model.TermsModel
import com.ddc.bansoogi.landing.view.LandingView

class LandingController(private val view: LandingView) {

    var termsModel = TermsModel()
    var profileModel = ProfileModel()

    fun updateServiceChecked(value: Boolean) {
        termsModel.serviceChecked = value
    }

    fun updatePrivacyChecked(value: Boolean) {
        termsModel.privacyChecked = value
    }

    fun updateHealthChecked(value: Boolean) {
        termsModel.healthChecked = value
    }

    fun proceedFromTerms() {
        if (termsModel.isAllAgreed()) {
            view.showProfileScreen()
        } else {
            view.showValidationError("필수 약관에 모두 동의해야 합니다.")
        }
    }
}