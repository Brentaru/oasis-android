package com.oasis.mobile.utils

import android.graphics.BitmapFactory
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL

fun ImageView.loadUrl(url: String) {
    if (url.isBlank()) {
        return
    }

    Thread {
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 12000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "OasisMobile/1.0")
            }
            val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
            if (bitmap != null) {
                post {
                    setImageBitmap(bitmap)
                    requestLayout()
                }
            }
        } catch (_: Exception) {
            // Keep the existing placeholder when the remote image cannot be loaded.
        }
    }.start()
}
