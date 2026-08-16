package com.sloosh.tv.ui.continue_watching

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.data.repository.MoviesRepository
import com.sloosh.tv.data.repository.PlaybackProgressStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContinueWatchingItem(
    val id: String,
    val mediaId: String,
    val kpId: Int?,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val isEpisode: Boolean,
    val season: Int?,
    val episode: Int?,
    val positionSec: Double,
    val durationSec: Double,
    val progressFraction: Float,
    val timeLabel: String,
    val remainingLabel: String,
    val entity: ProgressEntity
)

data class ContinueUiState(
    val isLoading: Boolean = true,
    val items: List<ContinueWatchingItem> = emptyList()
)

class ContinueViewModel(application: Application) : AndroidViewModel(application) {

    private val progressStore = PlaybackProgressStore(application)
    private val moviesRepository = MoviesRepository()

    private val _uiState = MutableStateFlow(ContinueUiState())
    val uiState: StateFlow<ContinueUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            progressStore.allProgress.collect { records ->
                val activeRecords = records
                    .filter { it.positionSec > 10 && !it.watched }
                    .sortedByDescending { it.updatedAtMs }

                val mappedItems = withContext(Dispatchers.IO) {
                    activeRecords.map { record ->
                        async {
                            val kpId = record.mediaId.replace("kp_", "").trim().toIntOrNull()
                            var title = record.title.ifEmpty { "Просмотр" }
                            var poster = record.posterUrl
                            var backdrop = record.backdropUrl

                            if (title == "Просмотр" || poster.isNullOrEmpty()) {
                                try {
                                    val details = moviesRepository.getDetails(record.mediaId)
                                    if (details != null) {
                                        title = details.displayTitle
                                        poster = details.getDisplayPosterUrl()
                                        backdrop = details.getDisplayBackdropUrl() ?: poster
                                    }
                                } catch (e: Exception) {}
                            }

                            val posSec = record.positionSec.toInt()
                            val durSec = record.durationSec.toInt()
                            val posStr = if (posSec >= 3600)
                                String.format("%d:%02d:%02d", posSec / 3600, (posSec % 3600) / 60, posSec % 60)
                            else
                                String.format("%02d:%02d", posSec / 60, posSec % 60)

                            val durStr = if (durSec >= 3600)
                                String.format("%d:%02d:%02d", durSec / 3600, (durSec % 3600) / 60, durSec % 60)
                            else
                                String.format("%02d:%02d", durSec / 60, durSec % 60)

                            val remainSec = (durSec - posSec).coerceAtLeast(0)
                            val remainMin = remainSec / 60
                            val remainText = if (remainMin >= 60)
                                "Осталось ${remainMin / 60} ч ${remainMin % 60} мин"
                            else
                                "Осталось $remainMin мин"

                            val fraction = if (durSec > 0) (posSec.toFloat() / durSec.toFloat()).coerceIn(0f, 1f) else 0f

                            ContinueWatchingItem(
                                id = record.mediaId,
                                mediaId = record.mediaId,
                                kpId = kpId,
                                title = title,
                                posterUrl = poster,
                                backdropUrl = backdrop ?: poster,
                                isEpisode = record.isEpisode,
                                season = record.season,
                                episode = record.episode,
                                positionSec = record.positionSec,
                                durationSec = record.durationSec,
                                progressFraction = fraction,
                                timeLabel = "$posStr / $durStr",
                                remainingLabel = remainText,
                                entity = record
                            )
                        }
                    }.awaitAll()
                }

                _uiState.value = ContinueUiState(
                    isLoading = false,
                    items = mappedItems
                )
            }
        }
    }

    fun markAsWatched(item: ContinueWatchingItem) {
        viewModelScope.launch {
            val updated = item.entity.copy(watched = true, updatedAtMs = System.currentTimeMillis())
            progressStore.saveProgress(updated)
        }
    }

    fun removeFromHistory(item: ContinueWatchingItem) {
        viewModelScope.launch {
            progressStore.deleteProgress(item.mediaId)
        }
    }
}
