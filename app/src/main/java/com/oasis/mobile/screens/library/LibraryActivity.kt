package com.oasis.mobile.screens.library

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.oasis.mobile.R
import com.oasis.mobile.data.MangaLibraryStore
import com.oasis.mobile.data.ReadingHistoryItem
import com.oasis.mobile.data.ReadingHistoryStore
import com.oasis.mobile.data.MangaSeries
import com.oasis.mobile.data.OasisApiClient
import com.oasis.mobile.screens.reader.ReaderActivity
import com.oasis.mobile.screens.browse.BrowseActivity
import com.oasis.mobile.screens.home.HomeActivity
import com.oasis.mobile.screens.profile.ProfileActivity
import com.oasis.mobile.screens.series.SeriesDetailsActivity
import com.oasis.mobile.utils.BottomNavTab
import com.oasis.mobile.utils.app
import com.oasis.mobile.utils.loadUrl
import com.oasis.mobile.utils.setupBottomNavIcons
import com.oasis.mobile.utils.start

class LibraryActivity : Activity() {
    private var showingHistory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        setupBottomNav()
        findViewById<TextView>(R.id.buttonSavedTab).setOnClickListener {
            showingHistory = false
            updateTabs()
            loadContent()
        }
        findViewById<TextView>(R.id.buttonHistoryTab).setOnClickListener {
            showingHistory = true
            updateTabs()
            loadContent()
        }
        loadContent()
    }

    override fun onResume() {
        super.onResume()
        loadContent()
    }

    private fun loadContent() {
        if (showingHistory) {
            loadHistory(findViewById(R.id.libraryList))
        } else {
            loadSavedLibrary(findViewById(R.id.libraryList))
        }
    }

    private fun loadSavedLibrary(container: LinearLayout) {
        val stateText = findViewById<TextView>(R.id.libraryStateText)
        container.removeAllViews()
        stateText.text = "Loading saved titles..."

        Thread {
            val series = try {
                val remote = OasisApiClient.getSavedTitles(app().getUserInfo())
                remote.forEach { MangaLibraryStore.save(this, it) }
                remote
            } catch (_: Exception) {
                MangaLibraryStore.getSaved(this)
            }

            runOnUiThread {
                container.removeAllViews()
                if (series.isEmpty()) {
                    stateText.text = "No saved titles yet. Browse MangaDex and save the ones you want."
                } else {
                    stateText.text = "${series.size} saved title${if (series.size == 1) "" else "s"}"
                    series.forEach { container.addView(libraryRow(it)) }
                }
            }
        }.start()
    }

    private fun loadHistory(container: LinearLayout) {
        val stateText = findViewById<TextView>(R.id.libraryStateText)
        container.removeAllViews()
        stateText.text = "Loading reading history..."

        Thread {
            val history = try {
                val remote = OasisApiClient.getReadingHistory(app().getUserInfo())
                remote.forEach { ReadingHistoryStore.record(this, it.series, it.chapter, it.lastPage) }
                remote
            } catch (_: Exception) {
                ReadingHistoryStore.getHistory(this)
            }

            runOnUiThread {
                container.removeAllViews()
                if (history.isEmpty()) {
                    stateText.text = "No reading history yet. Open a chapter to start tracking."
                } else {
                    stateText.text = "${history.size} recent chapter${if (history.size == 1) "" else "s"}"
                    history.forEach { container.addView(historyRow(it)) }
                }
            }
        }.start()
    }

    private fun libraryRow(series: MangaSeries): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_oasis_card)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { openSeriesDetails(series) }
        }
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) }

        row.addView(ImageView(this).apply {
            setBackgroundResource(R.drawable.bg_oasis_field)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(78), dp(112))
            loadUrl(series.coverImage)
        })

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        details.addView(TextView(this).apply {
            text = series.title
            setTextColor(getColor(R.color.oasis_text))
            textSize = 16f
            maxLines = 3
        })
        details.addView(TextView(this).apply {
            val genres = series.genres.take(3).joinToString(", ").ifBlank { series.genre }
            text = listOf(genres, series.status).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "Saved" }
            setTextColor(getColor(R.color.oasis_muted))
            textSize = 12f
            setPadding(0, dp(6), 0, 0)
        })
        row.addView(details)
        return row
    }

    private fun historyRow(item: ReadingHistoryItem): LinearLayout {
        val row = libraryRow(item.series)
        row.setOnClickListener { openReader(item) }
        val details = row.getChildAt(1) as? LinearLayout
        details?.addView(TextView(this).apply {
            text = "Chapter ${item.chapter.chapterNumber} / Page ${item.lastPage}"
            setTextColor(getColor(R.color.oasis_primary_soft))
            textSize = 12f
            setPadding(0, dp(8), 0, 0)
        })
        return row
    }

    private fun updateTabs() {
        findViewById<TextView>(R.id.buttonSavedTab).apply {
            setBackgroundResource(if (!showingHistory) R.drawable.bg_oasis_primary else 0)
            setTextColor(getColor(if (!showingHistory) R.color.white else R.color.oasis_muted))
        }
        findViewById<TextView>(R.id.buttonHistoryTab).apply {
            setBackgroundResource(if (showingHistory) R.drawable.bg_oasis_primary else 0)
            setTextColor(getColor(if (showingHistory) R.color.white else R.color.oasis_muted))
        }
    }

    private fun setupBottomNav() {
        setupBottomNavIcons(BottomNavTab.LIBRARY)
        findViewById<android.view.View>(R.id.navHome).setOnClickListener { start(HomeActivity::class.java) }
        findViewById<android.view.View>(R.id.navBrowse).setOnClickListener { start(BrowseActivity::class.java) }
        findViewById<android.view.View>(R.id.navProfile).setOnClickListener { start(ProfileActivity::class.java) }
    }

    private fun openSeriesDetails(series: MangaSeries) {
        startActivity(Intent(this, SeriesDetailsActivity::class.java).apply {
            putExtra(SeriesDetailsActivity.EXTRA_TITLE, series.title)
            putExtra(SeriesDetailsActivity.EXTRA_AUTHOR, series.author)
            putExtra(SeriesDetailsActivity.EXTRA_GENRE, series.genre)
            putExtra(SeriesDetailsActivity.EXTRA_GENRES, series.genres.joinToString("|"))
            putExtra(SeriesDetailsActivity.EXTRA_STATUS, series.status)
            putExtra(SeriesDetailsActivity.EXTRA_DESCRIPTION, series.description)
            putExtra(SeriesDetailsActivity.EXTRA_COVER_IMAGE, series.coverImage)
            putExtra(SeriesDetailsActivity.EXTRA_SERIES_ID, series.seriesId)
            putExtra(SeriesDetailsActivity.EXTRA_SOURCE_ID, series.sourceId)
            putExtra(SeriesDetailsActivity.EXTRA_SOURCE, series.source)
        })
    }

    private fun openReader(item: ReadingHistoryItem) {
        startActivity(Intent(this, ReaderActivity::class.java).apply {
            putExtra(ReaderActivity.EXTRA_SERIES_ID, item.series.seriesId)
            putExtra(ReaderActivity.EXTRA_SOURCE_ID, item.series.sourceId)
            putExtra(ReaderActivity.EXTRA_TITLE, item.series.title)
            putExtra(ReaderActivity.EXTRA_COVER_IMAGE, item.series.coverImage)
            putExtra(ReaderActivity.EXTRA_GENRE, item.series.genre)
            putExtra(ReaderActivity.EXTRA_STATUS, item.series.status)
            putExtra(ReaderActivity.EXTRA_CHAPTER_ID, item.chapter.chapterId)
            putExtra(ReaderActivity.EXTRA_CHAPTER_NUMBER, item.chapter.chapterNumber)
            putExtra(ReaderActivity.EXTRA_CHAPTER_TITLE, item.chapter.title)
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
