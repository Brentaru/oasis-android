package com.oasis.mobile.screens.browse

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.oasis.mobile.R
import com.oasis.mobile.data.MangaSeries
import com.oasis.mobile.data.OasisApiClient
import com.oasis.mobile.screens.home.HomeActivity
import com.oasis.mobile.screens.library.LibraryActivity
import com.oasis.mobile.screens.profile.ProfileActivity
import com.oasis.mobile.screens.series.SeriesDetailsActivity
import com.oasis.mobile.utils.BottomNavTab
import com.oasis.mobile.utils.loadUrl
import com.oasis.mobile.utils.setupBottomNavIcons
import com.oasis.mobile.utils.start

class BrowseActivity : Activity() {
    private var currentSeries: List<MangaSeries> = emptyList()
    private var gridView = false
    private val statusValues = listOf("", "ongoing", "completed", "hiatus", "cancelled")
    private val orderValues = listOf("followedCount", "latestUploadedChapter")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        setupBottomNav()
        setupFilters()
        findViewById<Button>(R.id.buttonSearch).setOnClickListener {
            loadMangaDex(findViewById<EditText>(R.id.edittextSearch).text.toString())
        }
        findViewById<TextView>(R.id.buttonListView).setOnClickListener {
            gridView = false
            updateViewToggle()
            renderResults()
        }
        findViewById<TextView>(R.id.buttonGridView).setOnClickListener {
            gridView = true
            updateViewToggle()
            renderResults()
        }

        loadMangaDex()
    }

    private fun setupFilters() {
        findViewById<Spinner>(R.id.spinnerStatus).adapter =
            spinnerAdapter(listOf("All status", "Ongoing", "Completed", "Hiatus", "Cancelled"))
        findViewById<Spinner>(R.id.spinnerOrder).adapter =
            spinnerAdapter(listOf("Popular", "Latest"))
    }

    private fun spinnerAdapter(values: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getView(position, convertView, parent) as TextView).apply {
                    setTextColor(getColor(R.color.oasis_text))
                    textSize = 13f
                }
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                    setTextColor(getColor(R.color.oasis_text))
                    setBackgroundColor(getColor(R.color.oasis_field))
                    textSize = 13f
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                }
            }
        }
    }

    private fun loadMangaDex(query: String = "") {
        val container = findViewById<LinearLayout>(R.id.browseList)
        val stateText = findViewById<TextView>(R.id.browseStateText)
        val selectedStatus = statusValues[findViewById<Spinner>(R.id.spinnerStatus).selectedItemPosition]
        val selectedOrder = orderValues[findViewById<Spinner>(R.id.spinnerOrder).selectedItemPosition]
        container.removeAllViews()
        stateText.text = if (query.isBlank()) "Loading MangaDex..." else "Searching MangaDex..."

        Thread {
            try {
                val series = OasisApiClient.browseMangaDex(
                    query = query,
                    status = selectedStatus,
                    order = selectedOrder
                )
                runOnUiThread {
                    currentSeries = series
                    if (series.isEmpty()) {
                        container.removeAllViews()
                        stateText.text = "No MangaDex titles found."
                    } else {
                        stateText.text = "${series.size} result${if (series.size == 1) "" else "s"}"
                        renderResults()
                    }
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    stateText.text = exception.message ?: "Unable to load MangaDex."
                }
            }
        }.start()
    }

    private fun renderResults() {
        val container = findViewById<LinearLayout>(R.id.browseList)
        container.removeAllViews()
        if (gridView) {
            currentSeries.chunked(2).forEach { rowItems ->
                container.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(12) }
                    rowItems.forEach { addView(gridCard(it)) }
                    if (rowItems.size == 1) {
                        addView(LinearLayout(this@BrowseActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                        })
                    }
                })
            }
        } else {
            currentSeries.forEach { container.addView(browseCard(it)) }
        }
    }

    private fun updateViewToggle() {
        findViewById<TextView>(R.id.buttonListView).apply {
            setBackgroundResource(if (!gridView) R.drawable.bg_oasis_primary else 0)
            setTextColor(getColor(if (!gridView) R.color.white else R.color.oasis_muted))
        }
        findViewById<TextView>(R.id.buttonGridView).apply {
            setBackgroundResource(if (gridView) R.drawable.bg_oasis_primary else 0)
            setTextColor(getColor(if (gridView) R.color.white else R.color.oasis_muted))
        }
    }

    private fun browseCard(series: MangaSeries): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_oasis_card)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { openSeriesDetails(series) }
        }
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) }

        card.addView(ImageView(this).apply {
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
            text = series.genres.take(3).joinToString(", ").ifBlank { series.genre.ifBlank { "MangaDex" } }
            setTextColor(getColor(R.color.oasis_muted))
            textSize = 12f
            setPadding(0, dp(6), 0, 0)
        })
        details.addView(TextView(this).apply {
            text = listOf(series.source, series.status).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "MangaDex" }
            setTextColor(getColor(R.color.oasis_primary_soft))
            textSize = 12f
            setPadding(0, dp(12), 0, 0)
        })

        card.addView(details)
        return card
    }

    private fun gridCard(series: MangaSeries): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_oasis_card)
            setPadding(dp(8), dp(8), dp(8), dp(10))
            setOnClickListener { openSeriesDetails(series) }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
                marginStart = dp(6)
            }
        }
        card.addView(ImageView(this).apply {
            setBackgroundResource(R.drawable.bg_oasis_field)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(210))
            loadUrl(series.coverImage)
        })
        card.addView(TextView(this).apply {
            text = series.title
            setTextColor(getColor(R.color.oasis_text))
            textSize = 14f
            maxLines = 2
            setPadding(0, dp(8), 0, 0)
        })
        card.addView(TextView(this).apply {
            text = (series.genres.firstOrNull() ?: series.genre).ifBlank { series.status.ifBlank { "MangaDex" } }
            setTextColor(getColor(R.color.oasis_muted))
            textSize = 11f
            maxLines = 1
            setPadding(0, dp(4), 0, 0)
        })
        return card
    }

    private fun setupBottomNav() {
        setupBottomNavIcons(BottomNavTab.BROWSE)
        findViewById<android.view.View>(R.id.navHome).setOnClickListener { start(HomeActivity::class.java) }
        findViewById<android.view.View>(R.id.navLibrary).setOnClickListener { start(LibraryActivity::class.java) }
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
