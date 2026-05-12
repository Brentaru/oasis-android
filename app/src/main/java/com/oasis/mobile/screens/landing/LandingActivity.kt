package com.oasis.mobile.screens.landing

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import com.oasis.mobile.R
import com.oasis.mobile.screens.login.LoginActivity
import com.oasis.mobile.screens.register.RegisterActivity
import com.oasis.mobile.utils.start

class LandingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        findViewById<Button>(R.id.buttonLogin).setOnClickListener { start(LoginActivity::class.java) }
        findViewById<Button>(R.id.buttonRegister).setOnClickListener { start(RegisterActivity::class.java) }
    }
}
