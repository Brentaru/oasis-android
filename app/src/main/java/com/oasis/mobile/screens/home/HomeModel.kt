package com.oasis.mobile.screens.home

import com.oasis.mobile.app.CustomApp
import com.oasis.mobile.data.UserInfo

class HomeModel(private val app: CustomApp) {

    fun getUserInfo(): UserInfo = app.getUserInfo()
}