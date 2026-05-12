package com.oasis.mobile.screens.login

interface LoginContract {

    interface View {
        fun showSuccessMessage()
        fun showInvalidCredentialMessage(message: String)
        fun showEmptyMessage()
        fun navigateToHomeScreen()
        fun showGenericErrorMessage(message: String)
        fun showLoading()
        fun hideLoading()
    }

    interface Presenter {
        fun login(username: String, password: String)
    }
}
