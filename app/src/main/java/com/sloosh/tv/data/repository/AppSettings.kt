package com.sloosh.tv.data.repository

import android.content.Context
import android.content.SharedPreferences

enum class DetailsScreenStyle(val id: String, val title: String) {
    CENTERED("centered", "По центру (iOS)"),
    SIDE_POSTER("side_poster", "С постером сбоку")
}

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sloosh_tv_settings", Context.MODE_PRIVATE)

    var detailsStyle: DetailsScreenStyle
        get() {
            val saved = prefs.getString("details_style", DetailsScreenStyle.CENTERED.id)
            return DetailsScreenStyle.values().firstOrNull { it.id == saved } ?: DetailsScreenStyle.CENTERED
        }
        set(value) {
            prefs.edit().putString("details_style", value.id).apply()
        }

    var isHighPosterQuality: Boolean
        get() = prefs.getBoolean("high_poster_quality", true)
        set(value) {
            prefs.edit().putBoolean("high_poster_quality", value).apply()
        }

    var isAutoplayEnabled: Boolean
        get() = prefs.getBoolean("autoplay_enabled", true)
        set(value) {
            prefs.edit().putBoolean("autoplay_enabled", value).apply()
        }

    var gridColumns: Int
        get() = prefs.getInt("grid_columns", 5).coerceIn(5, 6)
        set(value) {
            prefs.edit().putInt("grid_columns", value).apply()
        }
}
