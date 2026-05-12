package com.oasis.mobile.data

object ApiConfig {
    // Emulator: 10.0.2.2 points to the computer running Spring Boot.
    // Real phone: use your computer's Wi-Fi IPv4 address.
    val BASE_URLS = listOf(
        "http://192.168.1.5:8080/api",
        "http://10.0.2.2:8080/api"
    )

    var CURRENT_BASE_URL = BASE_URLS.first()
}
