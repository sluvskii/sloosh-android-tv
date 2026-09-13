package com.sloosh.tv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.data.repository.MoviesRepository
import com.sloosh.tv.data.repository.PlaybackProgressStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeCategory(val title: String) {
    ALL("Все"),
    MOVIES("Фильмы"),
    SERIES("Сериалы"),
    CARTOONS("Мультфильмы"),
    ANIME("Аниме")
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
    val categoryItems: Map<HomeCategory, List<MediaDto>> = emptyMap(),
    val categoryPages: Map<HomeCategory, Int> = emptyMap(),
    val errorMessage: String? = null
) {
    val items: List<MediaDto> get() = categoryItems[selectedCategory] ?: emptyList()
    val currentPage: Int get() = categoryPages[selectedCategory] ?: 1
    val hasMorePages: Boolean get() = items.isNotEmpty() || isLoading
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MoviesRepository.instance

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        loadData(reset = true)
        prefetchOtherCategories()
    }

    fun selectCategory(category: HomeCategory) {
        if (_uiState.value.selectedCategory == category) return
        val hasCachedItems = !_uiState.value.categoryItems[category].isNullOrEmpty()
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            isLoading = !hasCachedItems
        )
        if (!hasCachedItems) {
            loadData(reset = true)
        }
    }

    fun selectFilter(filter: HomeFilter) {
        if (_uiState.value.selectedFilter == filter) return
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            categoryItems = emptyMap(),
            categoryPages = emptyMap()
        )
        loadData(reset = true)
        prefetchOtherCategories()
    }

    fun loadData(reset: Boolean = false) {
        val targetCategory = _uiState.value.selectedCategory
        val targetFilter = _uiState.value.selectedFilter
        val targetPage = if (reset) 1 else (_uiState.value.categoryPages[targetCategory] ?: 1) + 1

        if (reset) {
            fetchJob?.cancel()
        } else {
            if (fetchJob?.isActive == true) return
        }

        fetchJob = viewModelScope.launch {
            if (reset && _uiState.value.categoryItems[targetCategory].isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    errorMessage = null
                )
            } else if (!reset) {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
            }

            try {
                val newItems = fetchCatalogPage(
                    category = targetCategory,
                    filter = targetFilter,
                    page = targetPage
                )

                val existing = if (reset) emptyList() else (_uiState.value.categoryItems[targetCategory] ?: emptyList())
                val combined = (existing + newItems)
                    .filter { it.identifier.isNotBlank() }
                    .distinctBy { it.identifier }
                val updatedCategoryItems = _uiState.value.categoryItems + (targetCategory to combined)
                val updatedCategoryPages = _uiState.value.categoryPages + (targetCategory to targetPage)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    categoryItems = updatedCategoryItems,
                    categoryPages = updatedCategoryPages
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
                coroutineScope {
                    if (filter == HomeFilter.POPULAR) {
                        val trendingDeferred = async(Dispatchers.IO) { repository.getTrending(page) }
                        val cartoonsDeferred = async(Dispatchers.IO) { repository.getCartoons(page) }
                        val animeDeferred = async(Dispatchers.IO) { repository.getAnime(page, "NUM_VOTE") }

                        val trending = trendingDeferred.await()
                        val cartoons = cartoonsDeferred.await()
                        val anime = animeDeferred.await()

                        if (trending.isEmpty() && cartoons.isEmpty() && anime.isEmpty()) {
                            repository.getPopularMovies(page)
                        } else {
                            interleaveMedia(
                                primary = trending,
                                secondary1 = cartoons,
                                secondary2 = anime
                            )
                        }
                    } else {
                        val moviesDeferred = async(Dispatchers.IO) { repository.getTopMovies(page) }
                        val tvDeferred = async(Dispatchers.IO) { repository.getTopTv(page) }
                        val cartoonsDeferred = async(Dispatchers.IO) { repository.getCartoons(page) }
                        val animeDeferred = async(Dispatchers.IO) { repository.getAnime(page, "RATING") }

                        val movies = moviesDeferred.await()
                        val tv = tvDeferred.await()
                        val cartoons = cartoonsDeferred.await()
                        val anime = animeDeferred.await()

                        interleaveFour(movies, tv, cartoons, anime)
                    }
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
                // getTopTv is a dedicated 100% TV series endpoint
                val raw = repository.getTopTv(page)
                val filtered = raw.filter { it.isTvSeries && !it.isCartoon }
                if (filtered.isEmpty()) raw else filtered
            }
            HomeCategory.CARTOONS -> {
                repository.getCartoons(page)
            }
            HomeCategory.ANIME -> {
                val order = if (filter == HomeFilter.POPULAR) "NUM_VOTE" else "RATING"
                repository.getAnime(page, order)
            }
        }
    }

    private fun interleaveMedia(
        primary: List<MediaDto>,
        secondary1: List<MediaDto>,
        secondary2: List<MediaDto>
    ): List<MediaDto> {
        val result = mutableListOf<MediaDto>()
        var pIdx = 0
        var s1Idx = 0
        var s2Idx = 0

        while (pIdx < primary.size || s1Idx < secondary1.size || s2Idx < secondary2.size) {
            repeat(4) {
                if (pIdx < primary.size) {
                    result.add(primary[pIdx++])
                }
            }
            if (s1Idx < secondary1.size) {
                result.add(secondary1[s1Idx++])
            }
            if (s2Idx < secondary2.size) {
                result.add(secondary2[s2Idx++])
            }
        }
        return result
    }

    private fun interleaveFour(
        list1: List<MediaDto>,
        list2: List<MediaDto>,
        list3: List<MediaDto>,
        list4: List<MediaDto>
    ): List<MediaDto> {
        val result = mutableListOf<MediaDto>()
        val maxSize = maxOf(list1.size, list2.size, list3.size, list4.size)
        for (i in 0 until maxSize) {
            if (i < list1.size) result.add(list1[i])
            if (i < list2.size) result.add(list2[i])
            if (i < list3.size) result.add(list3[i])
            if (i < list4.size) result.add(list4[i])
        }
        return result
    }

    private fun prefetchOtherCategories() {
        val otherCategories = HomeCategory.values().filter { it != _uiState.value.selectedCategory }
        val filter = _uiState.value.selectedFilter
        for (category in otherCategories) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val newItems = fetchCatalogPage(
                        category = category,
                        filter = filter,
                        page = 1
                    ).filter { it.identifier.isNotBlank() }.distinctBy { it.identifier }

                    _uiState.update { current ->
                        if (current.categoryItems[category].isNullOrEmpty()) {
                            current.copy(
                                categoryItems = current.categoryItems + (category to newItems),
                                categoryPages = current.categoryPages + (category to 1)
                            )
                        } else {
                            current
                        }
                    }
                } catch (_: Exception) {
                    // Background prefetch error ignored
                }
            }
        }
    }
}
