package com.sloosh.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.data.repository.MoviesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val heroItem: MediaDto? = null,
    val popularMovies: List<MediaDto> = emptyList(),
    val topMovies: List<MediaDto> = emptyList(),
    val topTv: List<MediaDto> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: MoviesRepository = MoviesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val popular = repository.getPopularMovies()
                val topMovies = repository.getTopMovies()
                val topTv = repository.getTopTv()

                val hero = popular.firstOrNull() ?: topMovies.firstOrNull()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    heroItem = hero,
                    popularMovies = popular,
                    topMovies = topMovies,
                    topTv = topTv
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
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
