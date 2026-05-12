package com.oasis.mobile.data

import org.json.JSONObject
import java.net.URLEncoder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

object OasisApiClient {
    fun register(email: String, password: String, confirmPassword: String): Result<Unit> {
        return requestAcrossBaseUrls("/auth/register", "POST", "") { connection ->
            val body = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("confirmPassword", confirmPassword)
                .toString()

            OutputStreamWriter(connection.outputStream).use { it.write(body) }
            val response = readResponse(connection)
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(response.ifBlank { "Registration failed." })
            }
        }
    }

    fun browseMangaDex(
        query: String = "",
        status: String = "",
        contentRating: String = "safe,suggestive",
        order: String = "followedCount"
    ): List<MangaSeries> {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val params = mutableListOf("limit=24", "order=${URLEncoder.encode(order, "UTF-8")}")
        if (encodedQuery.isNotBlank()) {
            params.add("query=$encodedQuery")
        }
        if (status.isNotBlank()) {
            params.add("status=${URLEncoder.encode(status, "UTF-8")}")
        }
        if (contentRating.isNotBlank()) {
            params.add("contentRating=${URLEncoder.encode(contentRating, "UTF-8")}")
        }

        val json = getArrayAcrossBaseUrls("/sources/mangadex/series?${params.joinToString("&")}")
        return List(json.length()) { index ->
            val item = json.getJSONObject(index)
            parseSeries(item)
        }
    }

    fun getMangaDexSeriesDetails(series: MangaSeries): MangaSeries {
        val json = getObjectAcrossBaseUrls("/sources/mangadex/series/${mangaDexId(series)}")
        val loaded = parseSeries(json)
        return loaded.copy(
            seriesId = loaded.seriesId.ifBlank { series.seriesId },
            sourceId = loaded.sourceId.ifBlank { series.sourceId },
            coverImage = loaded.coverImage.ifBlank { series.coverImage }
        )
    }

    fun getMangaDexChapters(series: MangaSeries): List<MangaChapter> {
        val mangaId = mangaDexId(series)
        val json = getArrayAcrossBaseUrls("/sources/mangadex/series/$mangaId/chapters")
        return List(json.length()) { index ->
            val item = json.getJSONObject(index)
            MangaChapter(
                chapterId = item.optString("chapterId"),
                chapterNumber = item.optInt("chapterNumber"),
                title = item.optString("title")
            )
        }.filter { it.chapterId.isNotBlank() && it.chapterNumber > 0 }
    }

    fun getMangaDexChapterPages(series: MangaSeries, chapter: MangaChapter): List<MangaPage> {
        val mangaId = mangaDexId(series)
        val json = getArrayAcrossBaseUrls("/sources/mangadex/series/$mangaId/chapters/${chapter.chapterId}/pages")
        return List(json.length()) { index ->
            val item = json.getJSONObject(index)
            MangaPage(
                pageId = item.optString("pageId"),
                pageNumber = item.optInt("pageNumber"),
                imageUrl = item.optString("imageUrl")
            )
        }.filter { it.imageUrl.isNotBlank() }
    }

    fun getSavedTitles(userInfo: UserInfo): List<MangaSeries> {
        if (userInfo.userId.isBlank()) {
            return emptyList()
        }
        val json = getArrayAcrossBaseUrls("/account-library/${userInfo.userId}/saved")
        return List(json.length()) { index -> parseSeries(json.getJSONObject(index)) }
    }

    fun saveTitle(userInfo: UserInfo, series: MangaSeries) {
        if (userInfo.userId.isBlank()) {
            return
        }
        val connection = openConnection("${ApiConfig.CURRENT_BASE_URL}/account-library/${userInfo.userId}/saved", "PUT", userInfo.accessToken)
        OutputStreamWriter(connection.outputStream).use { it.write(series.toJson().toString()) }
        val response = readResponse(connection)
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(response.ifBlank { "Unable to save title." })
        }
    }

    fun removeSavedTitle(userInfo: UserInfo, seriesId: String) {
        if (userInfo.userId.isBlank()) {
            return
        }
        val connection = openConnection(
            "${ApiConfig.CURRENT_BASE_URL}/account-library/${userInfo.userId}/saved/${URLEncoder.encode(seriesId, "UTF-8")}",
            "DELETE",
            userInfo.accessToken
        )
        val response = readResponse(connection)
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(response.ifBlank { "Unable to remove title." })
        }
    }

    fun getReadingHistory(userInfo: UserInfo): List<ReadingHistoryItem> {
        if (userInfo.userId.isBlank()) {
            return emptyList()
        }
        val json = getArrayAcrossBaseUrls("/account-library/${userInfo.userId}/history")
        return List(json.length()) { index ->
            val item = json.getJSONObject(index)
            ReadingHistoryItem(
                series = parseSeries(item),
                chapter = MangaChapter(
                    chapterId = item.optString("chapterId"),
                    chapterNumber = item.optInt("chapterNumber"),
                    title = item.optString("chapterTitle")
                ),
                lastPage = item.optInt("lastReadPage", 1)
            )
        }.filter { it.series.seriesId.isNotBlank() && it.chapter.chapterId.isNotBlank() }
    }

    fun saveReadingHistory(userInfo: UserInfo, series: MangaSeries, chapter: MangaChapter, lastPage: Int) {
        if (userInfo.userId.isBlank()) {
            return
        }
        val body = series.toJson()
            .put("chapterId", chapter.chapterId)
            .put("chapterNumber", chapter.chapterNumber)
            .put("chapterTitle", chapter.title)
            .put("lastReadPage", lastPage)
        val connection = openConnection("${ApiConfig.CURRENT_BASE_URL}/account-library/${userInfo.userId}/history", "PUT", userInfo.accessToken)
        OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
        val response = readResponse(connection)
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(response.ifBlank { "Unable to save reading history." })
        }
    }

    fun getProfile(userInfo: UserInfo): UserInfo {
        val connection = openConnection("${ApiConfig.CURRENT_BASE_URL}/profile/${userInfo.userId}", "GET", userInfo.accessToken)
        val response = readResponse(connection)
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(response.ifBlank { "Unable to load profile." })
        }

        val json = JSONObject(response)
        return userInfo.copy(
            username = json.optString("email", userInfo.username),
            firstname = json.optString("fullName", userInfo.firstname),
            profilePhoto = json.optString("profilePhoto", userInfo.profilePhoto)
        )
    }

    fun updateProfile(userInfo: UserInfo, displayName: String) {
        val connection = openConnection("${ApiConfig.CURRENT_BASE_URL}/profile/${userInfo.userId}", "PUT", userInfo.accessToken)
        val body = JSONObject()
            .put("fullName", displayName)
            .put("email", userInfo.username)
            .put("phone", "")
            .put("bio", "")
            .toString()

        OutputStreamWriter(connection.outputStream).use { it.write(body) }
        val response = readResponse(connection)
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(response.ifBlank { "Unable to save profile." })
        }
    }

    fun changePassword(
        userInfo: UserInfo,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        val connection = openConnection("${ApiConfig.CURRENT_BASE_URL}/profile/${userInfo.userId}/password", "PUT", userInfo.accessToken)
        val body = JSONObject()
            .put("currentPassword", currentPassword)
            .put("newPassword", newPassword)
            .put("confirmPassword", confirmPassword)
            .toString()

        OutputStreamWriter(connection.outputStream).use { it.write(body) }
        val response = readResponse(connection)
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(response.ifBlank { "Unable to change password." })
        }
    }

    fun uploadProfilePhoto(
        userInfo: UserInfo,
        bytes: ByteArray,
        fileName: String,
        contentType: String
    ): String {
        val boundary = "OasisBoundary${System.currentTimeMillis()}"
        val connection = (URL("${ApiConfig.CURRENT_BASE_URL}/profile/${userInfo.userId}/photo").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 12000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${userInfo.accessToken}")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        DataOutputStream(connection.outputStream).use { output ->
            output.writeBytes("--$boundary\r\n")
            output.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
            output.writeBytes("Content-Type: $contentType\r\n\r\n")
            output.write(bytes)
            output.writeBytes("\r\n--$boundary--\r\n")
        }

        val response = readResponse(connection)
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(response.ifBlank { "Unable to upload picture." })
        }
        return JSONObject(response).optString("profilePhoto")
    }


    fun deleteAccount(userInfo: UserInfo) {
        val connection = openConnection("${ApiConfig.CURRENT_BASE_URL}/profile/${userInfo.userId}", "DELETE", userInfo.accessToken)
        val response = readResponse(connection)
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(response.ifBlank { "Unable to delete account." })
        }
    }

    private fun openConnection(url: String, method: String, accessToken: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Content-Type", "application/json")
            if (accessToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            if (method == "POST" || method == "PUT") {
                doOutput = true
            }
        }
    }

    private fun <T> requestAcrossBaseUrls(
        path: String,
        method: String,
        accessToken: String,
        block: (HttpURLConnection) -> T
    ): Result<T> {
        var lastError: Exception? = null
        for (baseUrl in ApiConfig.BASE_URLS) {
            try {
                return Result.success(block(openConnection("$baseUrl$path", method, accessToken)))
            } catch (exception: Exception) {
                lastError = exception
            }
        }
        return Result.failure(lastError ?: IllegalStateException("Unable to connect to Oasis."))
    }

    private fun getArrayAcrossBaseUrls(path: String): org.json.JSONArray {
        var lastError: Exception? = null
        for (baseUrl in ApiConfig.BASE_URLS) {
            try {
                val connection = openConnection("$baseUrl$path", "GET", "")
                val response = readResponse(connection)
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException(response.ifBlank { "Unable to load data." })
                }
                ApiConfig.CURRENT_BASE_URL = baseUrl
                return org.json.JSONArray(response)
            } catch (exception: Exception) {
                lastError = exception
            }
        }
        throw lastError ?: IllegalStateException("Unable to connect to Oasis.")
    }

    private fun getObjectAcrossBaseUrls(path: String): JSONObject {
        var lastError: Exception? = null
        for (baseUrl in ApiConfig.BASE_URLS) {
            try {
                val connection = openConnection("$baseUrl$path", "GET", "")
                val response = readResponse(connection)
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException(response.ifBlank { "Unable to load data." })
                }
                ApiConfig.CURRENT_BASE_URL = baseUrl
                return JSONObject(response)
            } catch (exception: Exception) {
                lastError = exception
            }
        }
        throw lastError ?: IllegalStateException("Unable to connect to Oasis.")
    }

    private fun mangaDexId(series: MangaSeries): String {
        return series.sourceId.ifBlank { series.seriesId.removePrefix("mangadex:") }
    }

    private fun parseSeries(item: JSONObject): MangaSeries {
        val genres = item.optJSONArray("genres")?.let { array ->
            List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
        } ?: emptyList()

        return MangaSeries(
            seriesId = item.optString("seriesId"),
            sourceId = item.optString("sourceId"),
            title = item.optString("title", "Untitled"),
            author = item.optString("author"),
            artist = item.optString("artist"),
            genre = item.optString("genre", genres.firstOrNull().orEmpty()),
            genres = genres,
            status = item.optString("status"),
            contentRating = item.optString("contentRating"),
            year = item.optInt("year", 0),
            totalChapters = item.optInt("totalChapters", 0),
            latestChapterNumber = item.optInt("latestChapterNumber", 0),
            source = item.optString("source", "MangaDex"),
            coverImage = item.optString("coverImage"),
            description = item.optString("description")
        )
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        return BufferedReader(InputStreamReader(stream)).use { it.readText() }
    }
}
