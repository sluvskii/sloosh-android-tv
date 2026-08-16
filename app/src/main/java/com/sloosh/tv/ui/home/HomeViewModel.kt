package com.sloosh.tv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.data.repository.MoviesRepository
import com.sloosh.tv.data.repository.PlaybackProgressStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeCategory(val title: String) {
    ALL("Все"),
    MOVIES("Фильмы"),
    SERIES("Сериалы"),
    CARTOONS("Мультфильмы")
}

enum class HomeFilter(val title: String, val iconName: String) {
    POPULAR("Смотрят сейчас", "flame"),
    TOP_RATED("По рейтингу", "star")
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val selectedCategory: HomeCategory = HomeCategory.ALL,
    val selectedFilter: HomeFilter = HomeFilter.POPULAR,
    val items: List<MediaDto> = emptyList(),
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val errorMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MoviesRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        loadData(reset = true)
    }

    fun selectCategory(category: HomeCategory) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadData(reset = true)
    }

    fun selectFilter(filter: HomeFilter) {
        if (_uiState.value.selectedFilter == filter) return
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        loadData(reset = true)
    }

    fun loadData(reset: Boolean = false) {
        if (fetchJob?.isActive == true) return
        val currentState = _uiState.value
        if (!reset && (!currentState.hasMorePages || currentState.isLoadingMore)) return

        val nextPage = if (reset) 1 else currentState.currentPage + 1

        fetchJob = viewModelScope.launch {
            if (reset) {
                _uiState.value = currentState.copy(isLoading = true, currentPage = 1, items = emptyList(), errorMessage = null)
            } else {
                _uiState.value = currentState.copy(isLoadingMore = true)
            }

            try {
                val newItems = fetchCatalogPage(
                    category = _uiState.value.selectedCategory,
                    filter = _uiState.value.selectedFilter,
                    page = nextPage
                )

                val updatedList = if (reset) newItems else currentState.items + newItems

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    items = updatedList,
                    currentPage = nextPage,
                    hasMorePages = newItems.isNotEmpty()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    errorMessage = e.localizedMessage ?: "Ошибка загрузки"
                )
            }
        }
    }

    private suspend fun fetchCatalogPage(category: HomeCategory, filter: HomeFilter, page: Int): List<MediaDto> {
        return when (category) {
            HomeCategory.ALL -> {
                if (filter == HomeFilter.POPULAR) {
                    repository.getPopularMovies(page)
                } else {
                    repository.getTopMovies(page)
                }
            }
            HomeCategory.MOVIES -> {
                if (filter == HomeFilter.POPULAR) {
                    repository.getPopularMovies(page)
                } else {
                    repository.getTopMovies(page)
                }
            }
            HomeCategory.SERIES -> {
                if (filter == HomeFilter.POPULAR) {
                    repository.getTopTv(page)
                } else {
                    repository.getTopTv(page)
                }
            }
            HomeCategory.CARTOONS -> {
                repository.searchMovies("мультфильм", page)
            }
        }
    }
}
