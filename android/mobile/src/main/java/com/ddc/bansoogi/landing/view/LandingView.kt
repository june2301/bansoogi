package com.ddc.bansoogi.landing.view

interface LandingView {
    fun showStartScreen()
    fun showTermsScreen()
    fun showNicknameInputScreen()
    fun showBirthInputScreen()
    fun showTimeScreen()
    fun moveToMainActivity()
    fun showValidationError(errorString: String)
}