package com.sloosh.tv.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_progress")
data class ProgressEntity(
    @PrimaryKey val mediaId: String,
    val kpId: Int = 0,
    val season: Int? = null,
    val episode: Int? = null,
    val positionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val watched: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis(),
    val title: String = "",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val logoUrl: String? = null
) {
    val isEpisode: Boolean get() = season != null && episode != null

    val progressFraction: Float
        get() {
            if (durationSec <= 0 || positionSec <= 0) return 0f
            return (positionSec / durationSec).toFloat().coerceIn(0f, 0.99f)
        }
}

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: String,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val year: String?,
    val type: String?,
    val addedAtMs: Long = System.currentTimeMillis()
)
