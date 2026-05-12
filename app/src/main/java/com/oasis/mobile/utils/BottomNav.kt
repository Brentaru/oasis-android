package com.oasis.mobile.utils

import android.app.Activity
import android.widget.ImageView
import android.widget.TextView
import com.oasis.mobile.R

enum class BottomNavTab {
    HOME,
    LIBRARY,
    BROWSE,
    PROFILE
}

fun Activity.setupBottomNavIcons(activeTab: BottomNavTab) {
    setNavItem(R.id.navHomeIcon, R.id.navHomeLabel, activeTab == BottomNavTab.HOME, R.drawable.ic_nav_home, R.drawable.ic_nav_home_active)
    setNavItem(R.id.navLibraryIcon, R.id.navLibraryLabel, activeTab == BottomNavTab.LIBRARY, R.drawable.ic_nav_library, R.drawable.ic_nav_library_active)
    setNavItem(R.id.navBrowseIcon, R.id.navBrowseLabel, activeTab == BottomNavTab.BROWSE, R.drawable.ic_nav_browse, R.drawable.ic_nav_browse_active)
    setNavItem(R.id.navProfileIcon, R.id.navProfileLabel, activeTab == BottomNavTab.PROFILE, R.drawable.ic_nav_profile, R.drawable.ic_nav_profile_active)
}

private fun Activity.setNavItem(iconId: Int, labelId: Int, active: Boolean, icon: Int, activeIcon: Int) {
    findViewById<ImageView>(iconId).setImageResource(if (active) activeIcon else icon)
    findViewById<TextView>(labelId).setTextColor(getColor(if (active) R.color.oasis_primary else R.color.oasis_muted))
}
