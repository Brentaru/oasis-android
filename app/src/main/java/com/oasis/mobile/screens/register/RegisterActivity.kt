package com.oasis.mobile.screens.register

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.oasis.mobile.R
import com.oasis.mobile.data.OasisApiClient
import com.oasis.mobile.screens.login.LoginActivity
import com.oasis.mobile.utils.start
import com.oasis.mobile.utils.toast

class RegisterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        findViewById<TextView>(R.id.buttonLogin).setOnClickListener { start(LoginActivity::class.java) }
        findViewById<Button>(R.id.buttonRegister).setOnClickListener { register() }
    }

    private fun register() {
        val email = findViewById<EditText>(R.id.edittextEmail).text.toString().trim()
        val password = findViewById<EditText>(R.id.edittextPassword).text.toString()
        val confirmPassword = findViewById<EditText>(R.id.edittextConfirmPassword).text.toString()

        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            toast("All fields are required")
            return
        }
        if (password != confirmPassword) {
            toast("Passwords do not match")
            return
        }

        val button = findViewById<Button>(R.id.buttonRegister)
        button.isEnabled = false
        button.text = "Creating account..."

        Thread {
            val result = OasisApiClient.register(email, password, confirmPassword)
            runOnUiThread {
                button.isEnabled = true
                button.text = "Register"
                if (result.isSuccess) {
                    toast("Account created. Please login.")
                    start(LoginActivity::class.java)
                    finish()
                } else {
                    toast(result.exceptionOrNull()?.message ?: "Registration failed")
                }
            }
        }.start()
    }
}
