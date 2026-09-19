package com.sloosh.tv.data.repository

import android.content.Context
import android.content.SharedPreferences

enum class DetailsScreenStyle(val id: String, val title: String) {
    CENTERED("centered", "По центру (iOS)"),
    SIDE_POSTER("side_poster", "С постером сбоку")
}

enum class VideoQualityPreference(val id: String, val title: String, val shortTitle: String) {
    ASK("ask", "Спрашивать каждый раз", "Спрашивать"),
    AUTO("auto", "Авто (до 1080p)", "Авто"),
    Q1080("1080p", "1080p", "1080p"),
    Q720("720p", "720p", "720p"),
    Q480("480p", "480p", "480p"),
    Q360("360p", "360p", "360p");

    companion object {
        fun fromId(id: String?): VideoQualityPreference {
            if (id.isNullOrBlank()) return ASK
            return values().firstOrNull {
                it.id.equals(id, ignoreCase = true) ||
                it.name.equals(id, ignoreCase = true) ||
                it.title.equals(id, ignoreCase = true)
            } ?: ASK
        }
    }
}

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sloosh_tv_settings", Context.MODE_PRIVATE)

    var preferredQuality: VideoQualityPreference
        get() {
            val saved = prefs.getString("preferred_video_quality", VideoQualityPreference.ASK.id)
            return VideoQualityPreference.fromId(saved)
        }
        set(value) {
            prefs.edit().putString("preferred_video_quality", value.id).apply()
        }

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

