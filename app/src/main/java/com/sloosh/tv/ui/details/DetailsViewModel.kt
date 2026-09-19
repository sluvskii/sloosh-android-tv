package com.sloosh.tv.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.api.AllohaApiResult
import com.sloosh.tv.data.api.MediaDetailsDto
import com.sloosh.tv.data.api.MovieCollectionDto
import com.sloosh.tv.data.api.RelatedStudioResponse
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
import kotlinx.coroutines.withContext

data class DetailsUiState(
    val isLoading: Boolean = true,
    val details: MediaDetailsDto? = null,
    val progress: ProgressEntity? = null,
    val isFavorite: Boolean = false,
    val errorMessage: String? = null,
    val movieCollection: MovieCollectionDto? = null,
    val relatedStudio: RelatedStudioResponse? = null,

    // ─── Source selection sheet state ───────────────────────────
    val isFetchingSources: Boolean = false,
    val allohaData: AllohaApiResult? = null,
    val showSourceSheet: Boolean = false,
    val sourceFetchError: String? = null,
    val savedVoiceover: String? = null,
    val globalLastVoiceover: String? = null,
    val lastSeason: Int? = null,
    val lastEpisode: Int? = null
)

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MoviesRepository.instance
    val allohaRepository = AllohaRepository(application)
    private val store = PlaybackProgressStore(application)

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(mediaId: String) {
        val preview = repository.getPreviewDetails(mediaId)
        if (preview != null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                details = preview
            )
        } else {
            _uiState.value = _uiState.value.copy(isLoading = true)
        }

        viewModelScope.launch {
            val detailsDeferred = async { repository.getDetails(mediaId) }
            val progressDeferred = async { store.getProgress(mediaId) }
            val isFavDeferred = async { store.isFavorite(mediaId) }

            val details = detailsDeferred.await() ?: preview
            val progress = progressDeferred.await()
            val isFav = isFavDeferred.await()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                details = details,
                progress = progress,
                isFavorite = isFav
            )

            if (details != null) {
                val cleanRawId = mediaId.replace("kp_", "").replace("tv_", "").replace("movie_", "")
                val type = details.type ?: "movie"

                // 1. Franchise collection
                launch {
                    val existingCollection = details.collection
                    if (existingCollection != null && !existingCollection.parts.isNullOrEmpty()) {
                        _uiState.value = _uiState.value.copy(movieCollection = existingCollection)
                    } else if (!details.isTvSeries) {
                        val fetched = repository.getMovieCollection(cleanRawId)
                        if (fetched != null && !fetched.parts.isNullOrEmpty()) {
                            _uiState.value = _uiState.value.copy(movieCollection = fetched)
                        }
                    }
                }

                // 2. Related studio releases
                launch {
                    val studioResult = repository.getRelatedByStudio(type, cleanRawId)
                    if (studioResult != null) {
                        val items = studioResult.allItems
                        if (items.isNotEmpty()) {
                            val filtered = items.filter {
                                val itemCleanId = it.id.replace("kp_", "").replace("tv_", "").replace("movie_", "")
                                itemCleanId != cleanRawId && it.id != mediaId
                            }
                            if (filtered.isNotEmpty()) {
                                _uiState.value = _uiState.value.copy(
                                    relatedStudio = studioResult.copy(
                                        items = filtered.take(20)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** Called when the user presses "Смотреть" — opens the source sheet. */
    fun openSourceSheet() {
        val details = _uiState.value.details ?: return
        val mediaId = details.id ?: return
        val kpId = details.ids?.kp ?: mediaId.replace("kp_", "").trim().toIntOrNull()
        val imdbId = details.ids?.imdb
        val tmdbId = details.ids?.tmdb

        // Already have data — just show sheet
        if (_uiState.value.allohaData != null) {
            _uiState.value = _uiState.value.copy(showSourceSheet = true, sourceFetchError = null)
            return
        }

        _uiState.value = _uiState.value.copy(
            showSourceSheet = true,
            isFetchingSources = true,
            sourceFetchError = null
        )

        viewModelScope.launch {
            try {
                val result = allohaRepository.fetchAllohaData(
                    mediaId = mediaId,
                    explicitKpId = kpId,
                    imdbId = imdbId,
                    tmdbId = tmdbId
                )

                if (result != null) {
                    val savedVoiceover: String? = withContext(Dispatchers.IO) {
                        kpId?.let { allohaRepository.getLastVoiceover(it) }
                    }
                    val globalLastVoiceover: String? = withContext(Dispatchers.IO) {
                        allohaRepository.getLastTranslation()
                    }
                    val lastSeason: Int? = withContext(Dispatchers.IO) {
                        kpId?.let { allohaRepository.getLastSeason(it) }
                    } ?: _uiState.value.progress?.season
                    val lastEpisode: Int? = withContext(Dispatchers.IO) {
                        kpId?.let { allohaRepository.getLastEpisode(it) }
                    } ?: _uiState.value.progress?.episode

                    _uiState.value = _uiState.value.copy(
                        allohaData = result,
                        isFetchingSources = false,
                        sourceFetchError = null,
                        savedVoiceover = savedVoiceover,
                        globalLastVoiceover = globalLastVoiceover,
                        lastSeason = lastSeason,
                        lastEpisode = lastEpisode
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        allohaData = null,
                        isFetchingSources = false,
                        sourceFetchError = "Источники для просмотра не найдены"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    allohaData = null,
                    isFetchingSources = false,
                    sourceFetchError = "Не удалось загрузить источники"
                )
            }
        }
    }

    /** Called when the sheet is dismissed. */
    fun dismissSourceSheet() {
        _uiState.value = _uiState.value.copy(showSourceSheet = false, isFetchingSources = false, sourceFetchError = null)
    }

    /** Saves playback choice preferences in background IO thread */
    fun saveLastPlaybackChoice(kpId: Int?, translationName: String, season: Int?, episode: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (kpId != null) {
                allohaRepository.saveLastVoiceover(kpId, translationName)
                allohaRepository.saveLastPlayed(kpId, season, episode)
            }
            allohaRepository.saveLastTranslation(translationName)
        }
    }

    /** Resets alloha cache so the next openSourceSheet() re-fetches. */
    fun resetSourceSheet() {
        _uiState.value = _uiState.value.copy(
            allohaData = null,
            sourceFetchError = null,
            savedVoiceover = null,
            globalLastVoiceover = null,
            lastSeason = null,
            lastEpisode = null
        )
    }

    fun toggleFavorite() {
        val details = _uiState.value.details ?: return
        val mediaId = details.id ?: return
        viewModelScope.launch {
            val entity = FavoriteEntity(
                mediaId = mediaId,
                title = details.displayTitle,
                posterUrl = details.getDisplayPosterUrl(),
                rating = details.ratings?.kp ?: details.ratings?.imdb ?: details.ratings?.tmdb,
                year = details.year?.toString(),
                type = details.type
            )
            store.toggleFavorite(entity)
            val updatedIsFav = store.isFavorite(mediaId)
            _uiState.value = _uiState.value.copy(isFavorite = updatedIsFav)
        }
    }
}
