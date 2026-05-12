package com.oasis.mobile.screens.splash

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.oasis.mobile.R
import com.oasis.mobile.screens.landing.LandingActivity
import com.oasis.mobile.utils.start

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            start(LandingActivity::class.java)
            finish()
        }, 900)
    }
}
