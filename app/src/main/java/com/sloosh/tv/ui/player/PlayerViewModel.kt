package com.sloosh.tv.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.api.AllohaResolvedStream
import com.sloosh.tv.data.api.AudioVariant
import com.sloosh.tv.data.api.QualityVariant
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.data.repository.AllohaRepository
import com.sloosh.tv.data.repository.PlaybackProgressStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isLoading: Boolean = true,
    val resolvedStream: AllohaResolvedStream? = null,
    val currentVideoUrl: String? = null,
    val currentAudio: AudioVariant? = null,
    val currentQuality: QualityVariant? = null,
    val startPositionSec: Double = 0.0,
    val errorMessage: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val allohaRepository = AllohaRepository(application)
    private val progressStore = PlaybackProgressStore(application)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentMediaId: String = ""

    fun initPlayer(iframeUrl: String, mediaId: String) {
        currentMediaId = mediaId
        viewModelScope.launch {
            _uiState.value = PlayerUiState(isLoading = true)
            try {
                val savedProgress = progressStore.getProgress(mediaId)
                val savedPosition = savedProgress?.positionSec ?: 0.0

                val stream = allohaRepository.resolveStream(iframeUrl)
                val activeAudio = stream.audioVariants.firstOrNull()
                val activeQuality = stream.qualityVariants.lastOrNull() ?: activeAudio?.qualityVariants?.lastOrNull()
                val videoUrl = activeQuality?.url ?: stream.videoUrl

                _uiState.value = PlayerUiState(
                    isLoading = false,
                    resolvedStream = stream,
                    currentVideoUrl = videoUrl,
                    currentAudio = activeAudio,
                    currentQuality = activeQuality,
                    startPositionSec = savedPosition
                )
            } catch (e: Exception) {
                _uiState.value = PlayerUiState(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка разрешения видеопотока"
                )
            }
        }
    }

    fun selectAudioTrack(audio: AudioVariant) {
        val nextUrl = audio.url
        _uiState.value = _uiState.value.copy(
            currentAudio = audio,
            currentVideoUrl = nextUrl
        )
    }

    fun selectQuality(quality: QualityVariant) {
        _uiState.value = _uiState.value.copy(
            currentQuality = quality,
            currentVideoUrl = quality.url
        )
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        if (currentMediaId.isEmpty() || durationMs <= 0) return
        val posSec = positionMs / 1000.0
        val durSec = durationMs / 1000.0
        val isWatched = (posSec / durSec) >= 0.95

        viewModelScope.launch {
            val entity = ProgressEntity(
                mediaId = currentMediaId,
                positionSec = posSec,
                durationSec = durSec,
                watched = isWatched,
                updatedAtMs = System.currentTimeMillis()
            )
            progressStore.saveProgress(entity)
        }
    }
}
