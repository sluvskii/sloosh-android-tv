package com.sloosh.tv.ui.player

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloosh.tv.data.alloha.AllohaSessionHolder
import com.sloosh.tv.data.alloha.HlsProxyServer
import com.sloosh.tv.data.api.*
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.data.repository.AllohaRepository
import com.sloosh.tv.data.repository.AllohaRuntimeParser
import com.sloosh.tv.data.repository.AllohaRuntimeResolver
import com.sloosh.tv.data.repository.MoviesRepository
import com.sloosh.tv.data.repository.PlaybackProgressStore
import com.sloosh.tv.data.repository.allohaTranslationNamesMatch
import com.sloosh.tv.data.repository.findMatchingAudioVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

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

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentMediaId: String = ""
    private var currentIframeUrl: String = ""

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

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

                // Resolve stream using AllohaRuntimeResolver (mirrors iOS architecture)
                val resolvedStream = allohaRepository.resolveStream(iframeUrl)
                if (resolvedStream.videoUrl.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Не удалось получить видеопоток"
                    )
                    return@launch
                }

                val proxy = HlsProxyServer.shared
                proxy.start(resolvedStream.headers)
                proxy.updateHeaders(resolvedStream.headers)
                proxy.updateMasterUrl(resolvedStream.videoUrl)
                proxy.onSessionExpired = {
                    refreshSessionSilently()
                }
                val proxyUrl = if (resolvedStream.videoUrl.contains("127.0.0.1") || resolvedStream.videoUrl.contains("localhost")) {
                    resolvedStream.videoUrl
                } else {
                    proxy.proxyUrl(resolvedStream.videoUrl)
                }

                val ttl = resolvedStream.headers["x-neo-config-ttl"]?.toIntOrNull() ?: 360
                scheduleProactiveRefresh(iframeUrl, ttl)

                Log.d(TAG, "Stream resolved: masterUrl=${resolvedStream.videoUrl}, proxyUrl=$proxyUrl, ttl=$ttl")

                // Virtual subtitles via local proxy
                if (resolvedStream.subtitles.isNotEmpty()) {
                    proxy.subtitleTracks = resolvedStream.subtitles.map {
                        Triple(it.language, it.label, it.url)
                    }
                }

                val subtitleTracks = proxy.subtitleTracks.mapIndexed { index, triple ->
                    SubtitleTrack(
                        label = triple.second,
                        language = triple.first,
                        url = "http://127.0.0.1:${proxy.port}/sub/$index.vtt"
                    )
                }.ifEmpty { resolvedStream.subtitles }

                val qualities = resolvedStream.qualityVariants
                    .sortedByDescending { it.label.removeSuffix("p").toIntOrNull() ?: 0 }
                    .toMutableList()

                if (qualities.isEmpty()) {
                    qualities.add(QualityVariant(label = "Авто", url = resolvedStream.videoUrl))
                }

                val activeQuality = qualities.firstOrNull()

                // Gather available audio translations from catalog
                val allohaResult = _uiState.value.allohaData
                val audioVariants = mutableListOf<AudioVariant>()
                if (allohaResult != null) {
                    if (season != null && episode != null) {
                        val ep = allohaResult.seasons.firstOrNull { it.season == season }
                            ?.episodes?.firstOrNull { it.episode == episode }
                        ep?.translations?.forEach { tr ->
                            val itemUrl = tr.streamUrl ?: tr.iframeUrl
                            audioVariants.add(AudioVariant(id = tr.id, title = tr.name, url = itemUrl, qualityVariants = qualities))
                        }
                    } else {
                        allohaResult.movie?.translations?.forEach { tr ->
                            val itemUrl = tr.streamUrl ?: tr.iframeUrl
                            audioVariants.add(AudioVariant(id = tr.id, title = tr.name, url = itemUrl, qualityVariants = qualities))
                        }
                    }
                }
                if (audioVariants.isEmpty() && resolvedStream.audioVariants.isNotEmpty()) {
                    audioVariants.addAll(resolvedStream.audioVariants)
                }

                val kpIdInt = mediaId.removePrefix("kp_").toIntOrNull()
                val savedVoice = kpIdInt?.let { allohaRepository.getLastVoiceover(it) } ?: allohaRepository.getLastTranslation()
                val chosenAudio = findMatchingAudioVariant(audioVariants, savedVoice)
                    ?: audioVariants.firstOrNull { it.url == iframeUrl }
                    ?: audioVariants.firstOrNull()

                if (chosenAudio != null) {
                    allohaRepository.saveLastTranslation(chosenAudio.title)
                    if (kpIdInt != null) {
                        allohaRepository.saveLastVoiceover(kpIdInt, chosenAudio.title)
                    }
                }

                val activeStreamUrl = if (chosenAudio != null && chosenAudio.url.contains(".m3u8", ignoreCase = true)) {
                    chosenAudio.url
                } else {
                    resolvedStream.videoUrl
                }
                proxy.updateMasterUrl(activeStreamUrl)

                val effectiveProxyUrl = if (activeStreamUrl.contains("127.0.0.1") || activeStreamUrl.contains("localhost")) {
                    activeStreamUrl
                } else {
                    proxy.proxyUrl(activeStreamUrl)
                }

                val streamWithProxy = resolvedStream.copy(
                    videoUrl = effectiveProxyUrl,
                    qualityVariants = qualities,
                    audioVariants = audioVariants,
                    subtitles = subtitleTracks
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    resolvedStream = streamWithProxy,
                    currentVideoUrl = effectiveProxyUrl,
                    currentQuality = activeQuality,
                    currentAudio = chosenAudio,
                    currentSubtitle = null,
                    startPositionSec = savedPosition
                )

                // If quality variants are not provided in metadata, parse them from HLS master playlist in background
                if (qualities.size <= 1 && resolvedStream.videoUrl.contains(".m3u8", ignoreCase = true)) {
                    launch(Dispatchers.IO) {
                        try {
                            val reqBuilder = Request.Builder().url(resolvedStream.videoUrl)
                            resolvedStream.headers.forEach { (k, v) -> reqBuilder.header(k, v) }
                            val resp = httpClient.newCall(reqBuilder.build()).execute()
                            if (resp.isSuccessful) {
                                val body = resp.body?.string()
                                if (!body.isNullOrBlank()) {
                                    val parsedQualities = AllohaRuntimeParser.parseMasterPlaylistQualities(body, resolvedStream.videoUrl)
                                    if (parsedQualities.isNotEmpty()) {
                                        val updatedQualities = parsedQualities.sortedByDescending {
                                            it.label.removeSuffix("p").toIntOrNull() ?: 0
                                        }
                                        val currentStream = _uiState.value.resolvedStream
                                        if (currentStream != null) {
                                            _uiState.value = _uiState.value.copy(
                                                resolvedStream = currentStream.copy(qualityVariants = updatedQualities),
                                                currentQuality = updatedQualities.firstOrNull()
                                            )
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse master playlist qualities: ${e.message}")
                        }
                    }
                }

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
        if (audio.url == currentIframeUrl && _uiState.value.currentAudio?.id == audio.id) return
        currentIframeUrl = audio.url
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                currentAudio = audio
            )
            try {
                allohaRepository.saveLastTranslation(audio.title)
                val kpIdInt = currentMediaId.removePrefix("kp_").toIntOrNull()
                if (kpIdInt != null) {
                    allohaRepository.saveLastVoiceover(kpIdInt, audio.title)
                }

                // 1. If audio.url is already a direct playable stream URL
                if (audio.url.contains(".m3u8", ignoreCase = true) || audio.url.contains(".mp4", ignoreCase = true)) {
                    val proxy = HlsProxyServer.shared
                    proxy.updateMasterUrl(audio.url)
                    val newUrl = proxy.proxyUrl(audio.url)
                    val qualities = audio.qualityVariants.ifEmpty {
                        listOf(QualityVariant(label = "Авто", url = audio.url))
                    }
                    val activeQuality = qualities.firstOrNull { it.label == _uiState.value.currentQuality?.label }
                        ?: qualities.firstOrNull()
                    val updatedStream = _uiState.value.resolvedStream?.copy(
                        videoUrl = newUrl,
                        qualityVariants = qualities
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        resolvedStream = updatedStream,
                        currentVideoUrl = newUrl,
                        currentQuality = activeQuality
                    )
                    return@launch
                }

                // 2. Resolve iframe URL for the new translation
                AllohaRuntimeResolver.invalidateCache(audio.url)
                val resolvedStream = allohaRepository.resolveStream(audio.url)
                if (resolvedStream.videoUrl.isNotBlank()) {
                    val proxy = HlsProxyServer.shared
                    proxy.updateHeaders(resolvedStream.headers)
                    proxy.updateMasterUrl(resolvedStream.videoUrl)
                    proxy.onSessionExpired = {
                        refreshSessionSilently()
                    }
                    val newUrl = proxy.proxyUrl(resolvedStream.videoUrl)
                    val ttl = resolvedStream.headers["x-neo-config-ttl"]?.toIntOrNull() ?: 360
                    scheduleProactiveRefresh(audio.url, ttl)

                    if (resolvedStream.subtitles.isNotEmpty()) {
                        proxy.subtitleTracks = resolvedStream.subtitles.map {
                            Triple(it.language, it.label, it.url)
                        }
                    }

                    val subtitleTracks = proxy.subtitleTracks.mapIndexed { index, triple ->
                        SubtitleTrack(
                            label = triple.second,
                            language = triple.first,
                            url = "http://127.0.0.1:${proxy.port}/sub/$index.vtt"
                        )
                    }.ifEmpty { resolvedStream.subtitles }

                    val qualities = resolvedStream.qualityVariants
                        .sortedByDescending { it.label.removeSuffix("p").toIntOrNull() ?: 0 }
                        .ifEmpty { listOf(QualityVariant(label = "Авто", url = resolvedStream.videoUrl)) }

                    val activeQuality = qualities.firstOrNull { it.label == _uiState.value.currentQuality?.label }
                        ?: qualities.firstOrNull()

                    val updatedResolved = _uiState.value.resolvedStream?.copy(
                        videoUrl = newUrl,
                        qualityVariants = qualities,
                        subtitles = subtitleTracks
                    ) ?: resolvedStream.copy(
                        videoUrl = newUrl,
                        qualityVariants = qualities,
                        subtitles = subtitleTracks
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        resolvedStream = updatedResolved,
                        currentVideoUrl = newUrl,
                        currentQuality = activeQuality
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Не удалось сменить озвучку"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "selectAudioTrack error: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка переключения озвучки"
                )
            }
        }
    }

    fun selectQuality(quality: QualityVariant) {
        val proxy = HlsProxyServer.shared
        proxy.updateMasterUrl(quality.url)
        val newUrl = proxy.proxyUrl(quality.url)
        _uiState.value = _uiState.value.copy(
            currentQuality = quality,
            currentVideoUrl = newUrl
        )
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

    private var proactiveRefreshJob: Job? = null
    private var lastSilentRefreshTime = 0L

    fun scheduleProactiveRefresh(iframeUrl: String, ttlSeconds: Int) {
        proactiveRefreshJob?.cancel()
        proactiveRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            val safeTtl = ttlSeconds.coerceIn(60, 600)
            val delayMs = ((safeTtl - 25).coerceAtLeast(safeTtl / 2) * 1000L)
            Log.d(TAG, "Scheduling proactive stream refresh in ${delayMs / 1000}s (TTL=${safeTtl}s)")
            delay(delayMs)
            if (!isActive) return@launch

            Log.i(TAG, "Proactive stream refresh starting for: $iframeUrl")
            try {
                AllohaRuntimeResolver.invalidateCache(iframeUrl)
                val refreshed = allohaRepository.resolveStream(iframeUrl)
                if (refreshed.videoUrl.isNotBlank()) {
                    val proxy = HlsProxyServer.shared
                    proxy.updateHeaders(refreshed.headers)
                    proxy.updateMasterUrl(refreshed.videoUrl)
                    Log.i(TAG, "Proactive stream refresh successful. Master URL updated: ${refreshed.videoUrl}")
                    val nextTtl = refreshed.headers["x-neo-config-ttl"]?.toIntOrNull() ?: safeTtl
                    scheduleProactiveRefresh(iframeUrl, nextTtl)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Proactive stream refresh failed: ${e.message}")
                delay(15_000)
                if (isActive) {
                    scheduleProactiveRefresh(iframeUrl, 60)
                }
            }
        }
    }

    fun refreshSessionSilently() {
        val now = System.currentTimeMillis()
        if (now - lastSilentRefreshTime < 5000) return
        lastSilentRefreshTime = now
        val iframe = currentIframeUrl ?: return
        viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Refreshing session silently due to proxy signal...")
            try {
                AllohaRuntimeResolver.invalidateCache(iframe)
                val refreshed = allohaRepository.resolveStream(iframe)
                if (refreshed.videoUrl.isNotBlank()) {
                    val proxy = HlsProxyServer.shared
                    proxy.updateHeaders(refreshed.headers)
                    proxy.updateMasterUrl(refreshed.videoUrl)
                    Log.i(TAG, "Silent session refresh successful. Master URL updated: ${refreshed.videoUrl}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Silent session refresh failed: ${e.message}")
            }
        }
    }

    fun retryPlayback(positionMs: Long, onReady: ((String) -> Unit)? = null) {
        val iframe = currentIframeUrl ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                AllohaRuntimeResolver.invalidateCache(iframe)
                val refreshed = allohaRepository.resolveStream(iframe)
                if (refreshed.videoUrl.isNotBlank()) {
                    val proxy = HlsProxyServer.shared
                    proxy.updateHeaders(refreshed.headers)
                    proxy.updateMasterUrl(refreshed.videoUrl)
                    proxy.onSessionExpired = {
                        refreshSessionSilently()
                    }
                    val proxyUrl = if (refreshed.videoUrl.contains("127.0.0.1") || refreshed.videoUrl.contains("localhost")) {
                        refreshed.videoUrl
                    } else {
                        proxy.proxyUrl(refreshed.videoUrl)
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        currentVideoUrl = proxyUrl,
                        startPositionSec = positionMs / 1000.0
                    )

                    val ttl = refreshed.headers["x-neo-config-ttl"]?.toIntOrNull() ?: 360
                    scheduleProactiveRefresh(iframe, ttl)

                    onReady?.invoke(proxyUrl)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Не удалось обновить видеопоток"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка обновления потока"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        proactiveRefreshJob?.cancel()
        HlsProxyServer.shared.onSessionExpired = null
        AllohaSessionHolder.clear()
    }
}
