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
        if (reset) {
            fetchJob?.cancel()
        } else {
            if (fetchJob?.isActive == true) return
            val currentState = _uiState.value
            if (!currentState.hasMorePages || currentState.isLoadingMore) return
        }

        val targetCategory = _uiState.value.selectedCategory
        val targetFilter = _uiState.value.selectedFilter
        val targetPage = if (reset) 1 else _uiState.value.currentPage + 1

        fetchJob = viewModelScope.launch {
            if (reset) {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    currentPage = 1,
                    items = emptyList(),
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
            }

            try {
                val newItems = fetchCatalogPage(
                    category = targetCategory,
                    filter = targetFilter,
                    page = targetPage
                )

                val currentItems = if (reset) emptyList() else _uiState.value.items
                val updatedList = currentItems + newItems

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    items = updatedList,
                    currentPage = targetPage,
                    hasMorePages = newItems.isNotEmpty()
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
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
                val raw = if (filter == HomeFilter.POPULAR) {
                    repository.getPopularMovies(page)
                } else {
                    repository.getTopMovies(page)
                }
                raw.filter { it.isMovie }
            }
            HomeCategory.SERIES -> {
                // getTopTv is a dedicated 100% TV series endpoint with all top TV shows
                val raw = repository.getTopTv(page)
                val filtered = raw.filter { it.isTvSeries && !it.isCartoon }
                if (filtered.isEmpty()) raw else filtered
            }
            HomeCategory.CARTOONS -> {
                val raw = repository.searchMovies("мультфильм", page)
                val filtered = raw.filter { it.isCartoon || (it.title ?: it.name ?: "").contains("мульт", ignoreCase = true) }
                if (filtered.isEmpty()) raw else filtered
            }
        }
    }
}
