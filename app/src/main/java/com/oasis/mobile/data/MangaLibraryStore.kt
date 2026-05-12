package com.oasis.mobile.data

import android.content.Context
import org.json.JSONArray

object MangaLibraryStore {
    private const val PREFS = "oasis_library"
    private const val KEY_SAVED = "saved_series"

    fun getSaved(context: Context): List<MangaSeries> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SAVED, "[]") ?: "[]"
        val array = JSONArray(raw)
        val saved = List(array.length()) { index ->
            MangaSeries.fromJson(array.getJSONObject(index))
        }
        val mangaDexOnly = saved.filter { isMangaDex(it) }
        if (mangaDexOnly.size != saved.size) {
            write(context, mangaDexOnly)
        }
        return mangaDexOnly
    }

    fun isSaved(context: Context, seriesId: String): Boolean {
        return getSaved(context).any { it.seriesId == seriesId }
    }

    fun save(context: Context, series: MangaSeries) {
        if (!isMangaDex(series)) {
            return
        }
        val updated = getSaved(context)
            .filterNot { it.seriesId == series.seriesId }
            .toMutableList()
            .apply { add(0, series) }
        write(context, updated)
    }

    fun remove(context: Context, seriesId: String) {
        write(context, getSaved(context).filterNot { it.seriesId == seriesId })
    }

    private fun write(context: Context, series: List<MangaSeries>) {
        val array = JSONArray()
        series.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVED, array.toString())
            .apply()
    }

    private fun isMangaDex(series: MangaSeries): Boolean {
        return series.seriesId.startsWith("mangadex:") ||
            series.sourceId.isNotBlank() ||
            series.source.equals("mangadex", ignoreCase = true)
    }
}
