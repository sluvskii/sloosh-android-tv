package com.sloosh.tv.ui.player

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.alloha.AllohaSessionHolder
import com.sloosh.tv.data.alloha.AllohaSessionManager
import com.sloosh.tv.data.api.*
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.data.repository.AllohaRepository
import com.sloosh.tv.data.repository.MoviesRepository
import com.sloosh.tv.data.repository.PlaybackProgressStore
import com.sloosh.tv.data.repository.allohaTranslationNamesMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PlayerViewModel"

data class PlayerUiState(
    val isLoading: Boolean = true,
    val resolvedStream: AllohaResolvedStream? = null,
    val currentVideoUrl: String? = null,
    val currentAudio: AudioVariant? = null,
    val currentQuality: QualityVariant? = null,
    val currentSubtitle: SubtitleTrack? = null,
    val startPositionSec: Double = 0.0,
    val currentSeason: Int? = null,
    val currentEpisode: Int? = null,
    val mediaTitle: String? = null,
    val allohaData: AllohaApiResult? = null,
    val errorMessage: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val allohaRepository = AllohaRepository(application)
    private val progressStore = PlaybackProgressStore(application)
    private val moviesRepository = MoviesRepository()

    private var allohaSession: AllohaSessionManager? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentMediaId: String = ""
    private var currentIframeUrl: String = ""

    fun initPlayer(
        iframeUrl: String,
        mediaId: String,
        season: Int? = null,
        episode: Int? = null,
        initialTitle: String? = null
    ) {
        currentMediaId = mediaId
        currentIframeUrl = iframeUrl

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                currentSeason = season,
                currentEpisode = episode,
                mediaTitle = initialTitle?.takeIf { it != "Просмотр" && it.isNotBlank() } ?: _uiState.value.mediaTitle
            )

            // Load movie metadata in parallel to fetch real display title
            if (mediaId.isNotBlank()) {
                launch {
                    try {
                        val details = moviesRepository.getDetails(mediaId)
                        if (details != null) {
                            _uiState.value = _uiState.value.copy(
                                mediaTitle = details.displayTitle
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load movie details: ${e.message}")
                    }
                }
            }

            // Load alloha catalog in parallel if not loaded yet to enable episode navigation
            if (_uiState.value.allohaData == null && mediaId.isNotBlank()) {
                launch {
                    try {
                        val allohaResult = allohaRepository.fetchAllohaData(mediaId)
                        if (allohaResult != null) {
                            _uiState.value = _uiState.value.copy(
                                allohaData = allohaResult,
                                mediaTitle = _uiState.value.mediaTitle ?: allohaResult.title
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load alloha catalog: ${e.message}")
                    }
                }
            }

            try {
                val savedProgress = progressStore.getProgress(mediaId)
                val savedPosition = savedProgress?.positionSec ?: 0.0

                if (allohaSession == null) {
                    allohaSession = AllohaSessionManager(getApplication())
                }
                val session = allohaSession!!
                session.ensureInitialized()
                AllohaSessionHolder.session = session

                session.onStreamReady = { qualityMap, defaultUrl ->
                    val proxyUrl = "${session.proxyMasterUrl}?v=${System.currentTimeMillis()}"
                    Log.d(TAG, "Alloha stream ready: proxyUrl=$proxyUrl, qualities=${qualityMap.keys}")

                    val qualities = qualityMap.map { (k, v) ->
                        QualityVariant(label = "${k}p", url = v)
                    }.sortedByDescending { it.label.removeSuffix("p").toIntOrNull() ?: 0 }

                    val activeQuality = qualities.firstOrNull { it.label.removeSuffix("p") == session.lastSelectedQuality }
                        ?: qualities.firstOrNull()

                    // Gather available audio translations from catalog
                    val allohaResult = _uiState.value.allohaData
                    val audioVariants = mutableListOf<AudioVariant>()
                    if (allohaResult != null) {
                        if (season != null && episode != null) {
                            val ep = allohaResult.seasons.firstOrNull { it.season == season }
                                ?.episodes?.firstOrNull { it.episode == episode }
                            ep?.translations?.forEach { tr ->
                                audioVariants.add(AudioVariant(id = tr.id, title = tr.name, url = tr.iframeUrl, qualityVariants = qualities))
                            }
                        } else {
                            allohaResult.movie?.translations?.forEach { tr ->
                                audioVariants.add(AudioVariant(id = tr.id, title = tr.name, url = tr.iframeUrl, qualityVariants = qualities))
                            }
                        }
                    }

                    // Subtitles from proxy if present
                    val subtitleTracks = session.hlsProxy?.subtitleTracks?.mapIndexed { index, triple ->
                        SubtitleTrack(
                            label = triple.second,
                            language = triple.first,
                            url = "http://127.0.0.1:${session.hlsProxy?.port}/sub/$index.m3u8"
                        )
                    } ?: emptyList()

                    val resolvedStream = AllohaResolvedStream(
                        videoUrl = proxyUrl,
                        qualityVariants = qualities,
                        audioVariants = audioVariants,
                        subtitles = subtitleTracks,
                        headers = session.activeHeaders
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        resolvedStream = resolvedStream,
                        currentVideoUrl = proxyUrl,
                        currentQuality = activeQuality,
                        currentAudio = audioVariants.firstOrNull { it.url == iframeUrl } ?: audioVariants.firstOrNull(),
                        currentSubtitle = null,
                        startPositionSec = savedPosition
                    )
                }

                session.onError = { errorMsg ->
                    Log.e(TAG, "Alloha session error: $errorMsg")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }

                session.onM3u8Updated = { newUrl ->
                    Log.d(TAG, "Alloha upstream M3U8 updated: $newUrl")
                }

                session.startSession(iframeUrl)

            } catch (e: Exception) {
                Log.e(TAG, "initPlayer error: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка разрешения видеопотока"
                )
            }
        }
    }

    fun selectAudioTrack(audio: AudioVariant) {
        if (audio.url == currentIframeUrl) return
        currentIframeUrl = audio.url
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                currentAudio = audio
            )
            val session = allohaSession ?: AllohaSessionManager(getApplication()).also { allohaSession = it }
            session.ensureInitialized()
            session.onStreamReady = { qualityMap, defaultUrl ->
                val proxyUrl = "${session.proxyMasterUrl}?v=${System.currentTimeMillis()}"
                val qualities = qualityMap.map { (k, v) ->
                    QualityVariant(label = "${k}p", url = v)
                }.sortedByDescending { it.label.removeSuffix("p").toIntOrNull() ?: 0 }

                val activeQuality = qualities.firstOrNull { it.label.removeSuffix("p") == session.lastSelectedQuality }
                    ?: qualities.firstOrNull()

                val subtitleTracks = session.hlsProxy?.subtitleTracks?.mapIndexed { index, triple ->
                    SubtitleTrack(
                        label = triple.second,
                        language = triple.first,
                        url = "http://127.0.0.1:${session.hlsProxy?.port}/sub/$index.m3u8"
                    )
                } ?: emptyList()

                val resolvedStream = _uiState.value.resolvedStream?.copy(
                    videoUrl = proxyUrl,
                    qualityVariants = qualities,
                    subtitles = subtitleTracks
                ) ?: AllohaResolvedStream(
                    videoUrl = proxyUrl,
                    qualityVariants = qualities,
                    audioVariants = _uiState.value.resolvedStream?.audioVariants ?: emptyList(),
                    subtitles = subtitleTracks,
                    headers = session.activeHeaders
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    resolvedStream = resolvedStream,
                    currentVideoUrl = proxyUrl,
                    currentQuality = activeQuality
                )
            }
            session.startSession(audio.url)
        }
    }

    fun selectQuality(quality: QualityVariant) {
        val session = allohaSession ?: return
        val resolution = quality.label.removeSuffix("p")
        val switched = session.switchQuality(resolution)
        if (switched) {
            val newUrl = "${session.proxyMasterUrl}?q=$resolution&v=${System.currentTimeMillis()}"
            _uiState.value = _uiState.value.copy(
                currentQuality = quality,
                currentVideoUrl = newUrl
            )
        }
    }

    fun selectSubtitle(sub: SubtitleTrack?) {
        _uiState.value = _uiState.value.copy(
            currentSubtitle = sub
        )
    }

    fun getPrevEpisode(): Pair<Int, AllohaEpisode>? {
        val data = _uiState.value.allohaData ?: return null
        val curSeason = _uiState.value.currentSeason ?: return null
        val curEpisode = _uiState.value.currentEpisode ?: return null

        val seasonObj = data.seasons.firstOrNull { it.season == curSeason } ?: return null
        val prevEpInSeason = seasonObj.episodes.lastOrNull { it.episode < curEpisode }
        if (prevEpInSeason != null) return Pair(curSeason, prevEpInSeason)

        val prevSeason = data.seasons.filter { it.season < curSeason }.maxByOrNull { it.season } ?: return null
        val lastEpInPrevSeason = prevSeason.episodes.maxByOrNull { it.episode } ?: return null
        return Pair(prevSeason.season, lastEpInPrevSeason)
    }

    fun getNextEpisode(): Pair<Int, AllohaEpisode>? {
        val data = _uiState.value.allohaData ?: return null
        val curSeason = _uiState.value.currentSeason ?: return null
        val curEpisode = _uiState.value.currentEpisode ?: return null

        val seasonObj = data.seasons.firstOrNull { it.season == curSeason } ?: return null
        val nextEpInSeason = seasonObj.episodes.firstOrNull { it.episode > curEpisode }
        if (nextEpInSeason != null) return Pair(curSeason, nextEpInSeason)

        val nextSeason = data.seasons.filter { it.season > curSeason }.minByOrNull { it.season } ?: return null
        val firstEpInNextSeason = nextSeason.episodes.minByOrNull { it.episode } ?: return null
        return Pair(nextSeason.season, firstEpInNextSeason)
    }

    fun playNextEpisode() {
        val next = getNextEpisode() ?: return
        val curVoice = _uiState.value.currentAudio?.title
        val chosenTranslation = next.second.translations.firstOrNull { trans ->
            allohaTranslationNamesMatch(trans.name, curVoice)
        } ?: next.second.translations.firstOrNull() ?: return

        initPlayer(chosenTranslation.iframeUrl, currentMediaId, next.first, next.second.episode, _uiState.value.mediaTitle)
    }

    fun playPrevEpisode() {
        val prev = getPrevEpisode() ?: return
        val curVoice = _uiState.value.currentAudio?.title
        val chosenTranslation = prev.second.translations.firstOrNull { trans ->
            allohaTranslationNamesMatch(trans.name, curVoice)
        } ?: prev.second.translations.firstOrNull() ?: return

        initPlayer(chosenTranslation.iframeUrl, currentMediaId, prev.first, prev.second.episode, _uiState.value.mediaTitle)
    }

    private var lastSavedSec: Double = 0.0
    private var lastSavedTimeMs: Long = 0L

    fun saveProgress(positionMs: Long, durationMs: Long, force: Boolean = false) {
        if (currentMediaId.isEmpty() || durationMs <= 0) return
        val posSec = positionMs / 1000.0
        val durSec = durationMs / 1000.0
        val now = System.currentTimeMillis()

        if (!force && kotlin.math.abs(posSec - lastSavedSec) < 10.0 && (now - lastSavedTimeMs) < 10_000L) {
            return
        }

        lastSavedSec = posSec
        lastSavedTimeMs = now
        val isWatched = (posSec / durSec) >= 0.95

        viewModelScope.launch(Dispatchers.IO) {
            val entity = ProgressEntity(
                mediaId = currentMediaId,
                title = _uiState.value.mediaTitle ?: "",
                positionSec = posSec,
                durationSec = durSec,
                watched = isWatched,
                season = _uiState.value.currentSeason,
                episode = _uiState.value.currentEpisode,
                updatedAtMs = now
            )
            progressStore.saveProgress(entity)
        }
    }

    override fun onCleared() {
        super.onCleared()
        allohaSession?.release()
        allohaSession = null
        AllohaSessionHolder.clear()
    }
}
