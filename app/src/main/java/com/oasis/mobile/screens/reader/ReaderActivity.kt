package com.oasis.mobile.screens.reader

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.oasis.mobile.R
import com.oasis.mobile.data.MangaChapter
import com.oasis.mobile.data.MangaPage
import com.oasis.mobile.data.MangaSeries
import com.oasis.mobile.data.OasisApiClient
import com.oasis.mobile.data.ReadingHistoryStore
import com.oasis.mobile.utils.app
import com.oasis.mobile.utils.loadUrl

class ReaderActivity : Activity() {
    private lateinit var series: MangaSeries
    private lateinit var chapter: MangaChapter
    private var pages: List<MangaPage> = emptyList()
    private var pageIndex = 0
    private var readerMode = MODE_VERTICAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        series = MangaSeries(
            seriesId = intent.getStringExtra(EXTRA_SERIES_ID).orEmpty(),
            sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty(),
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            coverImage = intent.getStringExtra(EXTRA_COVER_IMAGE).orEmpty(),
            genre = intent.getStringExtra(EXTRA_GENRE).orEmpty(),
            status = intent.getStringExtra(EXTRA_STATUS).orEmpty(),
            source = "MangaDex"
        )
        chapter = MangaChapter(
            chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID).orEmpty(),
            chapterNumber = intent.getIntExtra(EXTRA_CHAPTER_NUMBER, 0),
            title = intent.getStringExtra(EXTRA_CHAPTER_TITLE).orEmpty()
        )

        findViewById<TextView>(R.id.buttonBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.textviewReaderTitle).text = "${series.title} - Chapter ${chapter.chapterNumber}"
        findViewById<LinearLayout>(R.id.chapterFooter).visibility = View.GONE
        findViewById<Button>(R.id.buttonPreviousChapter).isEnabled = false
        findViewById<Button>(R.id.buttonNextChapter).isEnabled = false
        findViewById<TextView>(R.id.buttonVerticalMode).setOnClickListener { setReaderMode(MODE_VERTICAL) }
        findViewById<TextView>(R.id.buttonPageMode).setOnClickListener { setReaderMode(MODE_PAGE) }
        findViewById<Button>(R.id.buttonPreviousPage).setOnClickListener { showPage(pageIndex - 1) }
        findViewById<Button>(R.id.buttonNextPage).setOnClickListener { showPage(pageIndex + 1) }
        loadPages()
    }

    private fun loadPages() {
        val container = findViewById<LinearLayout>(R.id.readerPages)
        val state = findViewById<TextView>(R.id.textviewReaderState)
        state.text = "Loading chapter pages..."

        Thread {
            try {
                val loadedPages = OasisApiClient.getMangaDexChapterPages(series, chapter)
                runOnUiThread {
                    pages = loadedPages
                    pageIndex = 0
                    if (pages.isEmpty()) {
                        container.removeAllViews()
                        container.addView(state.apply { text = "No pages found for this chapter." })
                    } else {
                        recordHistory(1)
                        renderPages()
                    }
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    state.text = exception.message ?: "Unable to load chapter pages."
                }
            }
        }.start()
    }

    private fun setReaderMode(mode: String) {
        readerMode = mode
        findViewById<TextView>(R.id.buttonVerticalMode).apply {
            setBackgroundResource(if (mode == MODE_VERTICAL) R.drawable.bg_oasis_primary else 0)
            setTextColor(getColor(if (mode == MODE_VERTICAL) R.color.white else R.color.oasis_muted))
        }
        findViewById<TextView>(R.id.buttonPageMode).apply {
            setBackgroundResource(if (mode == MODE_PAGE) R.drawable.bg_oasis_primary else 0)
            setTextColor(getColor(if (mode == MODE_PAGE) R.color.white else R.color.oasis_muted))
        }
        findViewById<LinearLayout>(R.id.pageControls).visibility = if (mode == MODE_PAGE) View.VISIBLE else View.GONE
        renderPages()
    }

    private fun renderPages() {
        if (pages.isEmpty()) return
        val container = findViewById<LinearLayout>(R.id.readerPages)
        container.removeAllViews()
        if (readerMode == MODE_PAGE) {
            showPage(pageIndex)
            return
        }

        pages.forEach { page ->
            container.addView(readerImage(page).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
            })
        }
        recordHistory(pages.size)
    }

    private fun showPage(targetIndex: Int) {
        if (pages.isEmpty()) return
        pageIndex = targetIndex.coerceIn(0, pages.lastIndex)
        val container = findViewById<LinearLayout>(R.id.readerPages)
        container.removeAllViews()
        container.addView(readerImage(pages[pageIndex]).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })
        findViewById<TextView>(R.id.textviewPageIndicator).text = "${pageIndex + 1} / ${pages.size}"
        findViewById<Button>(R.id.buttonPreviousPage).isEnabled = pageIndex > 0
        findViewById<Button>(R.id.buttonNextPage).isEnabled = pageIndex < pages.lastIndex
        recordHistory(pageIndex + 1)
    }

    private fun recordHistory(lastPage: Int) {
        ReadingHistoryStore.record(this, series, chapter, lastPage)
        Thread {
            try {
                OasisApiClient.saveReadingHistory(app().getUserInfo(), series, chapter, lastPage)
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun readerImage(page: MangaPage): ImageView {
        return ImageView(this).apply {
            adjustViewBounds = true
            minimumHeight = dp(320)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundResource(R.drawable.bg_oasis_card)
            loadUrl(page.imageUrl)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val MODE_VERTICAL = "Vertical"
        private const val MODE_PAGE = "Page"
        const val EXTRA_SERIES_ID = "series_id"
        const val EXTRA_SOURCE_ID = "source_id"
        const val EXTRA_TITLE = "series_title"
        const val EXTRA_COVER_IMAGE = "series_cover_image"
        const val EXTRA_GENRE = "series_genre"
        const val EXTRA_STATUS = "series_status"
        const val EXTRA_CHAPTER_ID = "chapter_id"
        const val EXTRA_CHAPTER_NUMBER = "chapter_number"
        const val EXTRA_CHAPTER_TITLE = "chapter_title"
    }
}
