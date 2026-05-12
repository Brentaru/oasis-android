package com.oasis.mobile.screens.home

class HomePresenter(private val view: HomeContract.View,val homeModel: HomeModel): HomeContract.Presenter {

    override fun navigateToProfileScreen() {
        view.navigateToProfileScreen()
    }

    override fun setWelcomeMessage() {
        if (homeModel.getUserInfo().username.isNotEmpty()) {
            view.setWelcomeMessage("Welcome back, ${homeModel.getUserInfo().username}")
        }
    }
}
