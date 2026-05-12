package com.oasis.mobile.screens.series

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.oasis.mobile.R
import com.oasis.mobile.data.MangaLibraryStore
import com.oasis.mobile.data.MangaChapter
import com.oasis.mobile.data.MangaSeries
import com.oasis.mobile.data.OasisApiClient
import com.oasis.mobile.screens.reader.ReaderActivity
import com.oasis.mobile.utils.app
import com.oasis.mobile.utils.loadUrl

class SeriesDetailsActivity : Activity() {
    private lateinit var series: MangaSeries
    private var chapters: List<MangaChapter> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_details)

        series = MangaSeries(
            seriesId = intent.getStringExtra(EXTRA_SERIES_ID).orEmpty(),
            sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty(),
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            author = intent.getStringExtra(EXTRA_AUTHOR).orEmpty(),
            genre = intent.getStringExtra(EXTRA_GENRE).orEmpty(),
            genres = intent.getStringExtra(EXTRA_GENRES).orEmpty().split("|").filter { it.isNotBlank() },
            status = intent.getStringExtra(EXTRA_STATUS).orEmpty(),
            source = intent.getStringExtra(EXTRA_SOURCE).orEmpty(),
            coverImage = intent.getStringExtra(EXTRA_COVER_IMAGE).orEmpty(),
            description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        )

        findViewById<TextView>(R.id.buttonBack).setOnClickListener { finish() }
        bindSeries()
        updateSaveButton()
        findViewById<Button>(R.id.buttonSave).setOnClickListener { toggleSaved() }
        findViewById<Button>(R.id.buttonRead).apply {
            isEnabled = false
            setOnClickListener { openChapter(chapters.lastOrNull()) }
        }
        loadSeriesDetails()
        loadChapters()
    }

    private fun toggleSaved() {
        if (MangaLibraryStore.isSaved(this, series.seriesId)) {
            MangaLibraryStore.remove(this, series.seriesId)
            Thread {
                try {
                    OasisApiClient.removeSavedTitle(app().getUserInfo(), series.seriesId)
                } catch (_: Exception) {
                }
            }.start()
        } else {
            MangaLibraryStore.save(this, series)
            Thread {
                try {
                    OasisApiClient.saveTitle(app().getUserInfo(), series)
                } catch (_: Exception) {
                }
            }.start()
        }
        updateSaveButton()
    }

    private fun updateSaveButton() {
        val saved = MangaLibraryStore.isSaved(this, series.seriesId)
        findViewById<Button>(R.id.buttonSave).text = if (saved) "Remove from Library" else "Save to Library"
    }

    private fun loadChapters() {
        val state = findViewById<TextView>(R.id.textviewChapterState)
        val list = findViewById<LinearLayout>(R.id.chapterList)
        state.text = "Loading chapters..."
        list.removeAllViews()

        Thread {
            try {
                val loaded = OasisApiClient.getMangaDexChapters(series)
                runOnUiThread {
                    chapters = loaded
                    findViewById<Button>(R.id.buttonRead).isEnabled = chapters.isNotEmpty()
                    list.removeAllViews()
                    if (chapters.isEmpty()) {
                        state.text = "No readable chapters found."
                    } else {
                        state.text = "${chapters.size} readable chapter${if (chapters.size == 1) "" else "s"}"
                        chapters.forEach { chapter -> list.addView(chapterRow(chapter)) }
                    }
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    findViewById<Button>(R.id.buttonRead).isEnabled = false
                    state.text = exception.message ?: "Unable to load chapters."
                }
            }
        }.start()
    }

    private fun loadSeriesDetails() {
        Thread {
            try {
                val loaded = OasisApiClient.getMangaDexSeriesDetails(series)
                runOnUiThread {
                    series = loaded
                    if (MangaLibraryStore.isSaved(this, series.seriesId)) {
                        MangaLibraryStore.save(this, series)
                    }
                    bindSeries()
                    updateSaveButton()
                }
            } catch (_: Exception) {
                // Keep the browse card data if the details request fails.
            }
        }.start()
    }

    private fun bindSeries() {
        findViewById<TextView>(R.id.textviewTitle).text = series.title.ifBlank { "Untitled" }
        findViewById<TextView>(R.id.textviewMeta).text = seriesMeta()
        findViewById<TextView>(R.id.textviewDescription).text = series.description.ifBlank {
            "Description is unavailable for this title."
        }
        findViewById<ImageView>(R.id.imageviewCover).loadUrl(series.coverImage)
    }

    private fun seriesMeta(): String {
        val creators = listOf(series.author, series.artist)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" / ")
        val genres = (series.genres.ifEmpty { listOf(series.genre) })
            .filter { it.isNotBlank() }
            .take(5)
            .joinToString(", ")
        return listOf(creators, genres, series.status)
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .ifBlank { "MangaDex" }
    }

    private fun chapterRow(chapter: MangaChapter): TextView {
        return TextView(this).apply {
            text = chapterLabel(chapter)
            setTextColor(getColor(R.color.oasis_text))
            textSize = 14f
            setBackgroundResource(R.drawable.bg_oasis_card)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            setOnClickListener { openChapter(chapter) }
        }
    }

    private fun openChapter(chapter: MangaChapter?) {
        if (chapter == null) {
            return
        }

        startActivity(android.content.Intent(this, ReaderActivity::class.java).apply {
            putExtra(ReaderActivity.EXTRA_SERIES_ID, series.seriesId)
            putExtra(ReaderActivity.EXTRA_SOURCE_ID, series.sourceId)
            putExtra(ReaderActivity.EXTRA_TITLE, series.title)
            putExtra(ReaderActivity.EXTRA_COVER_IMAGE, series.coverImage)
            putExtra(ReaderActivity.EXTRA_GENRE, series.genre)
            putExtra(ReaderActivity.EXTRA_STATUS, series.status)
            putExtra(ReaderActivity.EXTRA_CHAPTER_ID, chapter.chapterId)
            putExtra(ReaderActivity.EXTRA_CHAPTER_NUMBER, chapter.chapterNumber)
            putExtra(ReaderActivity.EXTRA_CHAPTER_TITLE, chapter.title)
        })
    }

    private fun chapterLabel(chapter: MangaChapter): String {
        return listOf("Chapter ${chapter.chapterNumber}", chapter.title)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_SERIES_ID = "series_id"
        const val EXTRA_SOURCE_ID = "source_id"
        const val EXTRA_TITLE = "series_title"
        const val EXTRA_AUTHOR = "series_author"
        const val EXTRA_GENRE = "series_genre"
        const val EXTRA_GENRES = "series_genres"
        const val EXTRA_STATUS = "series_status"
        const val EXTRA_DESCRIPTION = "series_description"
        const val EXTRA_COVER_IMAGE = "series_cover_image"
        const val EXTRA_SOURCE = "series_source"
    }
}
