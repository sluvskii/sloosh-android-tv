package com.sloosh.tv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.data.repository.MoviesRepository
import com.sloosh.tv.data.repository.PlaybackProgressStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val heroItem: MediaDto? = null,
    val continueWatching: List<ProgressEntity> = emptyList(),
    val popularMovies: List<MediaDto> = emptyList(),
    val topMovies: List<MediaDto> = emptyList(),
    val topTv: List<MediaDto> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MoviesRepository()
    private val progressStore = PlaybackProgressStore(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
        observeProgress()
    }

    private fun observeProgress() {
        viewModelScope.launch {
            progressStore.allProgress.collect { progress ->
                val recent = progress.filter { it.positionSec > 10 && !it.watched }.take(10)
                _uiState.value = _uiState.value.copy(continueWatching = recent)
            }
        }
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val popular = repository.getPopularMovies()
                val topMovies = repository.getTopMovies()
                val topTv = repository.getTopTv()

                val hero = popular.firstOrNull() ?: topMovies.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    heroItem = hero,
                    popularMovies = popular,
                    topMovies = topMovies,
                    topTv = topTv
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка загрузки каталога"
                )
            }
        }
    }

    fun selectHeroItem(item: MediaDto) {
        _uiState.value = _uiState.value.copy(heroItem = item)
    }
}
