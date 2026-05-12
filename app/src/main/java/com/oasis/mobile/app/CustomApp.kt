package com.oasis.mobile.app

import android.app.Application
import android.util.Log
import com.oasis.mobile.data.UserInfo

class CustomApp : Application() {

    val username = "test"
    val password = "test"

    private var userInfo = UserInfo()

    override fun onCreate() {
        super.onCreate()
        Log.e("CustomApp", "CustomApp:onCreate() is called")
    }

    fun getUserInfo() = this.userInfo

    fun setUserInfo(userInfo: UserInfo) {
        this.userInfo = userInfo
    }

}
