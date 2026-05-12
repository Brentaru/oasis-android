package com.oasis.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ReadingHistoryItem(
    val series: MangaSeries,
    val chapter: MangaChapter,
    val lastPage: Int = 1
) {
    fun toJson() = JSONObject()
        .put("series", series.toJson())
        .put("chapterId", chapter.chapterId)
        .put("chapterNumber", chapter.chapterNumber)
        .put("chapterTitle", chapter.title)
        .put("lastPage", lastPage)

    companion object {
        fun fromJson(item: JSONObject) = ReadingHistoryItem(
            series = MangaSeries.fromJson(item.getJSONObject("series")),
            chapter = MangaChapter(
                chapterId = item.optString("chapterId"),
                chapterNumber = item.optInt("chapterNumber"),
                title = item.optString("chapterTitle")
            ),
            lastPage = item.optInt("lastPage", 1)
        )
    }
}

object ReadingHistoryStore {
    private const val PREFS = "oasis_history"
    private const val KEY_ITEMS = "items"

    fun getHistory(context: Context): List<ReadingHistoryItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return List(array.length()) { index ->
            ReadingHistoryItem.fromJson(array.getJSONObject(index))
        }.filter { it.series.seriesId.startsWith("mangadex:") && it.chapter.chapterId.isNotBlank() }
    }

    fun record(context: Context, series: MangaSeries, chapter: MangaChapter, lastPage: Int) {
        val updated = getHistory(context)
            .filterNot { it.series.seriesId == series.seriesId && it.chapter.chapterId == chapter.chapterId }
            .toMutableList()
            .apply { add(0, ReadingHistoryItem(series, chapter, lastPage)) }
            .take(20)

        val array = JSONArray()
        updated.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
    }
}
