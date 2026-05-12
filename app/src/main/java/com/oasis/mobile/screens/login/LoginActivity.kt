package com.oasis.mobile.screens.login

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.oasis.mobile.R
import com.oasis.mobile.screens.home.HomeActivity
import com.oasis.mobile.screens.register.RegisterActivity
import com.oasis.mobile.utils.getButtonView
import com.oasis.mobile.utils.getEditTextValue
import com.oasis.mobile.utils.start
import com.oasis.mobile.utils.toast

class LoginActivity : Activity(), LoginContract.View {

    private lateinit var presenter: LoginPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        presenter = LoginPresenter(this, LoginModel())

        getButtonView(R.id.buttonLogin).setOnClickListener {
            val username = getEditTextValue(R.id.edittextUsername)
            val password = getEditTextValue(R.id.edittextPassword)

            presenter.login(username, password)
        }

        findViewById<TextView>(R.id.buttonRegister).setOnClickListener {
            start(RegisterActivity::class.java)
        }
    }

    override fun showSuccessMessage() {
        toast("Login successful!")
    }

    override fun showInvalidCredentialMessage(message: String) {
        toast(message)
    }

    override fun showEmptyMessage() {
        toast("Fields cannot be empty!")
    }

    override fun navigateToHomeScreen() {
        start(HomeActivity::class.java)
        finish()
    }

    override fun showGenericErrorMessage(message: String) {
        toast(message)
    }

    override fun showLoading() {
        getButtonView(R.id.buttonLogin).isEnabled = false
        getButtonView(R.id.buttonLogin).text = "Logging in..."
    }

    override fun hideLoading() {
        getButtonView(R.id.buttonLogin).isEnabled = true
        getButtonView(R.id.buttonLogin).text = "Login"
    }
}
