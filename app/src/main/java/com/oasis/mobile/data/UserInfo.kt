package com.oasis.mobile.data

data class UserInfo(
    var username: String = "",
    var password: String = "",
    var firstname: String = "",
    var lastname: String = "",
    var userId: String = "",
    var accessToken: String = "",
    var refreshToken: String = "",
    var profilePhoto: String = "",
    var readerMode: String = "Vertical"
)

data class MangaSeries(
    val seriesId: String = "",
    val sourceId: String = "",
    val title: String,
    val author: String = "",
    val artist: String = "",
    val genre: String = "",
    val genres: List<String> = emptyList(),
    val status: String = "",
    val contentRating: String = "",
    val year: Int = 0,
    val totalChapters: Int = 0,
    val latestChapterNumber: Int = 0,
    val source: String = "",
    val coverImage: String = "",
    val description: String = "",
    val progress: Int = 0
) {
    fun toJson() = org.json.JSONObject()
        .put("seriesId", seriesId)
        .put("sourceId", sourceId)
        .put("title", title)
        .put("author", author)
        .put("artist", artist)
        .put("genre", genre)
        .put("genres", org.json.JSONArray(genres))
        .put("status", status)
        .put("contentRating", contentRating)
        .put("year", year)
        .put("totalChapters", totalChapters)
        .put("latestChapterNumber", latestChapterNumber)
        .put("source", source)
        .put("coverImage", coverImage)
        .put("description", description)
        .put("progress", progress)

    companion object {
        fun fromJson(item: org.json.JSONObject) = MangaSeries(
            seriesId = item.optString("seriesId"),
            sourceId = item.optString("sourceId"),
            title = item.optString("title", "Untitled"),
            author = item.optString("author"),
            artist = item.optString("artist"),
            genre = item.optString("genre"),
            genres = item.optJSONArray("genres")?.let { array ->
                List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
            } ?: emptyList(),
            status = item.optString("status"),
            contentRating = item.optString("contentRating"),
            year = item.optInt("year", 0),
            totalChapters = item.optInt("totalChapters", 0),
            latestChapterNumber = item.optInt("latestChapterNumber", 0),
            source = item.optString("source"),
            coverImage = item.optString("coverImage"),
            description = item.optString("description"),
            progress = item.optInt("progress", 0)
        )
    }
}

data class MangaChapter(
    val chapterId: String,
    val chapterNumber: Int,
    val title: String = ""
)

data class MangaPage(
    val pageId: String,
    val pageNumber: Int,
    val imageUrl: String
)
