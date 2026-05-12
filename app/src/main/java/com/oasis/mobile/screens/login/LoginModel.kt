package com.oasis.mobile.screens.login

import com.oasis.mobile.data.ApiConfig
import com.oasis.mobile.data.UserInfo
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LoginModel {
    fun login(email: String, password: String, callback: (LoginResult) -> Unit) {
        Thread {
            var lastError = "Unable to connect to Oasis."
            for (baseUrl in ApiConfig.BASE_URLS) {
                try {
                    val connection = (URL("$baseUrl/auth/login").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 12000
                        readTimeout = 12000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                    }

                    val body = JSONObject()
                        .put("email", email)
                        .put("password", password)
                        .toString()

                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(body)
                    }

                    val responseText = readResponse(connection)
                    if (connection.responseCode !in 200..299) {
                        callback(LoginResult.Invalid(cleanMessage(responseText.ifBlank { "Invalid email or password." })))
                        return@Thread
                    }

                    val json = JSONObject(responseText)
                    val user = json.optJSONObject("user")
                    val userId = user?.optString("id").orEmpty()
                    val accessToken = json.optString("access_token", json.optString("accessToken"))
                    val refreshToken = json.optString("refresh_token", json.optString("refreshToken"))
                    ApiConfig.CURRENT_BASE_URL = baseUrl

                    callback(
                        LoginResult.Success(
                            UserInfo(
                                username = email,
                                password = password,
                                userId = userId,
                                accessToken = accessToken,
                                refreshToken = refreshToken
                            )
                        )
                    )
                    return@Thread
                } catch (exception: Exception) {
                    lastError = exception.message ?: "Unable to connect to $baseUrl"
                }
            }
            callback(LoginResult.Error(lastError))
        }.start()
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        return BufferedReader(InputStreamReader(stream)).use { reader ->
            reader.readText()
        }
    }
}

private fun cleanMessage(message: String): String {
    return message.trim().removeSurrounding("\"").ifBlank { "Invalid email or password." }
}

sealed class LoginResult {
    data class Success(val userInfo: UserInfo) : LoginResult()
    data class Invalid(val message: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}
