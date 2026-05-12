package com.oasis.mobile.screens.home

class HomeContract {
    interface View {
        fun navigateToProfileScreen()
        fun setWelcomeMessage(message: String)
    }

    interface Presenter {
        fun navigateToProfileScreen()
        fun setWelcomeMessage()
    }
}
