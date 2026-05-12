package com.oasis.mobile.screens.login

import android.app.Activity
import com.oasis.mobile.utils.app

class LoginPresenter(private  val view: LoginContract.View,
                     private val loginModel: LoginModel)
    : LoginContract.Presenter {

    private val app = (view as Activity).app()
    override fun login(username: String, password: String) {
        if (username.isNotEmpty() && password.isNotEmpty()) {
            view.showLoading()
            loginModel.login(username, password) { result ->
                (view as Activity).runOnUiThread {
                    view.hideLoading()
                    when (result) {
                        is LoginResult.Success -> {
                            app.setUserInfo(result.userInfo)
                            view.showSuccessMessage()
                            view.navigateToHomeScreen()
                        }
                        is LoginResult.Invalid -> view.showInvalidCredentialMessage(result.message)
                        is LoginResult.Error -> view.showGenericErrorMessage(result.message)
                    }
                }
            }
        } else {
            view.showEmptyMessage()
        }
    }
}
