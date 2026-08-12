package com.sloosh.tv.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.api.AllohaApiResult
import com.sloosh.tv.data.api.MediaDetailsDto
import com.sloosh.tv.data.db.FavoriteEntity
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.data.repository.AllohaRepository
import com.sloosh.tv.data.repository.MoviesRepository
import com.sloosh.tv.data.repository.PlaybackProgressStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailsUiState(
    val isLoading: Boolean = true,
    val details: MediaDetailsDto? = null,
    val allohaData: AllohaApiResult? = null,
    val progress: ProgressEntity? = null,
    val isFavorite: Boolean = false,
    val selectedSeason: Int = 1,
    val selectedEpisode: Int = 1,
    val errorMessage: String? = null
)

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MoviesRepository()
    private val allohaRepository = AllohaRepository(application)
    private val store = PlaybackProgressStore(application)

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(mediaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val details = repository.getDetails(mediaId)
            val alloha = allohaRepository.fetchAllohaData(mediaId)
            val progress = store.getProgress(mediaId)
            val isFav = store.isFavorite(mediaId)

            _uiState.value = DetailsUiState(
                isLoading = false,
                details = details,
                allohaData = alloha,
                progress = progress,
                isFavorite = isFav
            )
        }
    }

    fun toggleFavorite() {
        val details = _uiState.value.details ?: return
        val mediaId = details.id ?: return
        viewModelScope.launch {
            val entity = FavoriteEntity(
                mediaId = mediaId,
                title = details.title ?: "Без названия",
                posterUrl = details.getDisplayPosterUrl(),
                rating = details.ratings?.kp,
                year = details.year?.toString(),
                type = details.type
            )
            store.toggleFavorite(entity)
            val updatedIsFav = store.isFavorite(mediaId)
            _uiState.value = _uiState.value.copy(isFavorite = updatedIsFav)
        }
    }

    fun selectSeason(season: Int) {
        _uiState.value = _uiState.value.copy(selectedSeason = season, selectedEpisode = 1)
    }

    fun selectEpisode(episode: Int) {
        _uiState.value = _uiState.value.copy(selectedEpisode = episode)
    }
}
