package com.oasis.mobile.screens.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.oasis.mobile.R
import com.oasis.mobile.data.MangaLibraryStore
import com.oasis.mobile.data.MangaSeries
import com.oasis.mobile.data.OasisApiClient
import com.oasis.mobile.screens.browse.BrowseActivity
import com.oasis.mobile.screens.library.LibraryActivity
import com.oasis.mobile.screens.profile.ProfileActivity
import com.oasis.mobile.screens.series.SeriesDetailsActivity
import com.oasis.mobile.utils.BottomNavTab
import com.oasis.mobile.utils.app
import com.oasis.mobile.utils.loadUrl
import com.oasis.mobile.utils.setTextViewText
import com.oasis.mobile.utils.setupBottomNavIcons
import com.oasis.mobile.utils.start

class HomeActivity : Activity(), HomeContract.View {

    private lateinit var presenter: HomePresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        presenter = HomePresenter(this, HomeModel(app()))

        presenter.setWelcomeMessage()
        updateProfileButton()
        renderLibraryPreview()
        loadDiscover()
        setupBottomNav()

        findViewById<TextView>(R.id.buttonProfile).setOnClickListener {
            presenter.navigateToProfileScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        updateProfileButton()
        renderLibraryPreview()
    }

    override fun navigateToProfileScreen() {
        start(ProfileActivity::class.java)
    }

    override fun setWelcomeMessage(message: String) {
        setTextViewText(R.id.textviewWelcome, message)
    }

    private fun renderLibraryPreview() {
        val container = findViewById<LinearLayout>(R.id.libraryPreview)
        val continueContainer = findViewById<LinearLayout>(R.id.continueContainer)
        val stateText = findViewById<TextView>(R.id.homeStateText)
        container.removeAllViews()
        continueContainer.removeAllViews()
        stateText.text = "Loading your library..."

        Thread {
            val saved = try {
                val remote = OasisApiClient.getSavedTitles(app().getUserInfo())
                remote.forEach { MangaLibraryStore.save(this, it) }
                remote
            } catch (_: Exception) {
                MangaLibraryStore.getSaved(this)
            }

            runOnUiThread {
                container.removeAllViews()
                continueContainer.removeAllViews()
                if (saved.isEmpty()) {
                    stateText.text = "Your library is empty. Open Browse and save a title to begin."
                    return@runOnUiThread
                }

                stateText.text = "Continue reading"
                continueContainer.addView(continueCard(saved.first()))
                saved.drop(1).take(3).forEach { container.addView(seriesRow(it)) }
                if (saved.size == 1) {
                    container.addView(TextView(this).apply {
                        text = "More saved titles will appear here."
                        setTextColor(getColor(R.color.oasis_muted))
                        textSize = 13f
                        setPadding(0, dp(4), 0, dp(8))
                    })
                }
            }
        }.start()
    }

    private fun continueCard(series: MangaSeries): LinearLayout {
        return seriesRow(series)
    }

    private fun seriesRow(series: MangaSeries): LinearLayout {
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

    private fun loadDiscover() {
        val container = findViewById<LinearLayout>(R.id.discoverPreview)
        container.removeAllViews()
        container.addView(TextView(this).apply {
            text = "Loading MangaDex picks..."
            setTextColor(getColor(R.color.oasis_muted))
            textSize = 13f
        })

        Thread {
            try {
                val discover = OasisApiClient.browseMangaDex().take(4)
                runOnUiThread {
                    container.removeAllViews()
                    discover.forEach { container.addView(seriesRow(it)) }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    container.removeAllViews()
                    container.addView(TextView(this).apply {
                        text = "Discover is unavailable right now."
                        setTextColor(getColor(R.color.oasis_muted))
                        textSize = 13f
                    })
                }
            }
        }.start()
    }

    private fun setupBottomNav() {
        setupBottomNavIcons(BottomNavTab.HOME)
        findViewById<android.view.View>(R.id.navLibrary).setOnClickListener { start(LibraryActivity::class.java) }
        findViewById<android.view.View>(R.id.navBrowse).setOnClickListener { start(BrowseActivity::class.java) }
        findViewById<android.view.View>(R.id.navProfile).setOnClickListener { start(ProfileActivity::class.java) }
    }

    private fun updateProfileButton() {
        val user = app().getUserInfo()
        val label = user.firstname.ifBlank { user.username }.firstOrNull()?.toString()?.uppercase() ?: "O"
        findViewById<TextView>(R.id.buttonProfile).text = label
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
