package com.sloosh.tv.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.db.FavoriteEntity
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.data.repository.PlaybackProgressStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val favorites: List<FavoriteEntity> = emptyList(),
    val progressList: List<ProgressEntity> = emptyList()
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val store = PlaybackProgressStore(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            store.allFavorites.collect { favs ->
                _uiState.value = _uiState.value.copy(favorites = favs)
            }
        }
        viewModelScope.launch {
            store.allProgress.collect { prog ->
                _uiState.value = _uiState.value.copy(progressList = prog)
            }
        }
    }
}
