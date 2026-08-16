package com.sloosh.tv.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.data.db.AppDatabase
import com.sloosh.tv.data.db.SearchHistoryEntity
import com.sloosh.tv.data.repository.MoviesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<MediaDto> = emptyList(),
    val recentSearches: List<SearchHistoryEntity> = emptyList(),
    val errorMessage: String? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MoviesRepository()
    private val searchHistoryDao = AppDatabase.getDatabase(application).searchHistoryDao()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            searchHistoryDao.getRecentSearches().collect { history ->
                _uiState.value = _uiState.value.copy(recentSearches = history)
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        searchJob?.cancel()
        if (newQuery.trim().isEmpty()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _uiState.value = _uiState.value.copy(isLoading = true)
            val results = repository.searchMovies(newQuery.trim())
            _uiState.value = _uiState.value.copy(isLoading = false, results = results)
            if (newQuery.trim().length >= 2) {
                searchHistoryDao.insertSearch(SearchHistoryEntity(query = newQuery.trim()))
            }
        }
    }

    fun selectHistoryQuery(query: String) {
        onQueryChanged(query)
    }

    fun deleteHistoryQuery(query: String) {
        viewModelScope.launch {
            searchHistoryDao.deleteSearch(query)
        }
    }
}
