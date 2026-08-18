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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailsUiState(
    val isLoading: Boolean = true,
    val details: MediaDetailsDto? = null,
    val progress: ProgressEntity? = null,
    val isFavorite: Boolean = false,
    val errorMessage: String? = null,

    // ─── Source selection sheet state ───────────────────────────
    val isFetchingSources: Boolean = false,
    val allohaData: AllohaApiResult? = null,
    val showSourceSheet: Boolean = false
)

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MoviesRepository()
    val allohaRepository = AllohaRepository(application)
    private val store = PlaybackProgressStore(application)

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(mediaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val detailsDeferred = async { repository.getDetails(mediaId) }
            val progressDeferred = async { store.getProgress(mediaId) }
            val isFavDeferred = async { store.isFavorite(mediaId) }

            val details = detailsDeferred.await()
            val progress = progressDeferred.await()
            val isFav = isFavDeferred.await()

            _uiState.value = DetailsUiState(
                isLoading = false,
                details = details,
                progress = progress,
                isFavorite = isFav
            )

            // Pre-fetch seasons/episodes for series
            if (details != null && (details.type == "tv" || details.type == "series" || details.type == "serial")) {
                val kpId = details.ids?.kp
                val dId = details.id ?: mediaId
                launch {
                    val alloha = allohaRepository.fetchAllohaData(dId, kpId)
                    if (alloha != null) {
                        _uiState.value = _uiState.value.copy(allohaData = alloha)
                    }
                }
            }
        }
    }

    /** Called when the user presses "Смотреть" — opens the source sheet. */
    fun openSourceSheet() {
        val details = _uiState.value.details ?: return
        val mediaId = details.id ?: return
        val kpId = details.ids?.kp

        // Already have data — just show sheet
        if (_uiState.value.allohaData != null) {
            _uiState.value = _uiState.value.copy(showSourceSheet = true)
            return
        }

        _uiState.value = _uiState.value.copy(
            showSourceSheet = true,
            isFetchingSources = true
        )

        viewModelScope.launch {
            val result = allohaRepository.fetchAllohaData(mediaId, kpId)
            _uiState.value = _uiState.value.copy(
                allohaData = result,
                isFetchingSources = false
            )
        }
    }

    /** Called when the sheet is dismissed. */
    fun dismissSourceSheet() {
        _uiState.value = _uiState.value.copy(showSourceSheet = false)
    }

    /** Resets alloha cache so the next openSourceSheet() re-fetches. */
    fun resetSourceSheet() {
        _uiState.value = _uiState.value.copy(allohaData = null)
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
}
