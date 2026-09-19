package com.sloosh.tv.ui.player

import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.components.SlooshFocusableCard
import com.sloosh.tv.ui.theme.*
import com.sloosh.tv.data.alloha.HlsProxyServer
import com.sloosh.tv.data.repository.allohaTranslationNamesMatch
import java.util.Locale
import kotlinx.coroutines.delay

enum class PlayerHudState {
    PLAY, PAUSE, REWIND, FORWARD
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    iframeUrl: String,
    mediaId: String,
    onBack: () -> Unit,
    title: String = "Просмотр",
    season: Int? = null,
    episode: Int? = null,
    selectedVoice: String? = null,
    directStreamUrl: String? = null,
    initialQuality: String? = null,
    viewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var showAudioDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showResumeBadge by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var hudState by remember { mutableStateOf<PlayerHudState?>(null) }
    var lastPreservedPositionMs by remember { mutableStateOf<Long?>(null) }

    val isAnyModalOpen = showAudioDialog || showQualityDialog || showSubtitleDialog

    // Dedicated Focus Requesters
    val playFocusRequester = remember { FocusRequester() }
    val seekbarFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }
    val modalFocusRequester = remember { FocusRequester() }
    val errorFocusRequester = remember { FocusRequester() }

    var hasAutoRetried by remember { mutableStateOf(false) }

    LaunchedEffect(iframeUrl, season, episode, title, selectedVoice, directStreamUrl, initialQuality) {
        viewModel.initPlayer(iframeUrl, mediaId, season, episode, title, selectedVoice, directStreamUrl, initialQuality)
    }

    val exoPlayer = remember(context) {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setAllowInvalidateSelectionsOnRendererCapabilitiesChange(true)
                .setPreferredAudioLanguage("ru")
                .setPreferredTextLanguage("ru")
                .setSelectUndeterminedTextLanguage(true)
                .setMaxVideoSize(1920, 1088)
                .build()
        }

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 20_000,
                /* maxBufferMs = */ 90_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build().apply {
                setAudioAttributes(audioAttributes, true)
                volume = 1.0f
                setPauseAtEndOfMediaItems(false)
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var playerError by remember { mutableStateOf<String?>(null) }
    val activeError = state.errorMessage ?: playerError

    val displayTitle = state.mediaTitle?.takeIf { it.isNotBlank() }
        ?: title.takeIf { it != "Просмотр" && it.isNotBlank() }
        ?: state.allohaData?.title
        ?: "Просмотр"

    val skipActionFocusRequester = remember { FocusRequester() }
    var hasSkippedIntroForCurrentSession by remember(state.currentVideoUrl) { mutableStateOf(false) }
    var showIntroPrompt by remember { mutableStateOf(false) }

    val introRange = state.resolvedStream?.introRange
    val outroRange = state.resolvedStream?.outroRange
    val posSec = currentPositionMs / 1000.0
    val isInIntro = introRange != null && posSec in introRange.start..introRange.end && !hasSkippedIntroForCurrentSession
    val isInOutro = outroRange != null && posSec in outroRange.start..outroRange.end
    val hasSkipAction = isInIntro || (isInOutro && outroRange != null)

    // Reset skipped flag if user rewinds back before intro start
    LaunchedEffect(posSec, introRange) {
        if (introRange != null && posSec < introRange.start) {
            hasSkippedIntroForCurrentSession = false
        }
    }

    // Auto-prompt for Skip Intro on TV (shows for 7 seconds when entering intro while watching)
    LaunchedEffect(isInIntro, showControls) {
        if (isInIntro && !showControls && !hasSkippedIntroForCurrentSession) {
            showIntroPrompt = true
            delay(7000)
            showIntroPrompt = false
        } else if (!isInIntro || showControls) {
            showIntroPrompt = false
        }
    }

    // Trap focus inside error dialog or modal when opened, or restore to play/pause when controls appear
    LaunchedEffect(showControls, isAnyModalOpen, activeError) {
        if (activeError != null) {
            delay(120)
            try {
                errorFocusRequester.requestFocus()
            } catch (e: Exception) {}
        } else if (isAnyModalOpen) {
            delay(120)
            try {
                modalFocusRequester.requestFocus()
            } catch (e: Exception) {}
        } else if (showControls) {
            delay(80)
            try {
                playFocusRequester.requestFocus()
            } catch (e: Exception) {}
        } else {
            delay(50)
            try {
                rootFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    // Position tracker and buffering watchdog
    LaunchedEffect(exoPlayer) {
        var bufferingSeconds = 0
        while (true) {
            currentPositionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            if (durationMs > 0) {
                viewModel.saveProgress(currentPositionMs, durationMs)
            }

            // Watchdog: detect if playback is stalled in buffering for > 12s
            if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_BUFFERING && exoPlayer.playWhenReady) {
                bufferingSeconds++
                if (bufferingSeconds >= 12) {
                    bufferingSeconds = 0
                    val pos = exoPlayer.currentPosition
                    Log.w("PlayerScreen", "Buffering watchdog: stalled for 12s, kicking ExoPlayer at $pos ms")
                    exoPlayer.seekTo(pos)
                    exoPlayer.prepare()
                }
            } else {
                bufferingSeconds = 0
            }

            delay(1000)
        }
    }

    // Lifecycle observer: pause on background and ensure proxy is alive on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    }
                    viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration, force = true)
                }
                Lifecycle.Event.ON_RESUME -> {
                    HlsProxyServer.shared.start()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-hide controls after 5 seconds of playback without modal open
    LaunchedEffect(showControls, isPlaying, isAnyModalOpen) {
        if (showControls && isPlaying && !isAnyModalOpen) {
            delay(5000)
            showControls = false
        }
    }

    // Hide HUD action indicator after 1.2 seconds
    LaunchedEffect(hudState) {
        if (hudState != null) {
            delay(1200)
            hudState = null
        }
    }

    // Player events listener with auto-recovery
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.w("PlayerScreen", "ExoPlayer error: ${error.errorCodeName} - ${error.message}")
                val pos = exoPlayer.currentPosition
                if (!hasAutoRetried) {
                    hasAutoRetried = true
                    val resumePos = pos.coerceAtLeast(0L)
                    lastPreservedPositionMs = resumePos
                    Log.i("PlayerScreen", "Auto-recovering playback once at position $resumePos ms...")
                    viewModel.retryPlayback(resumePos) { newUrl ->
                        val mediaItem = androidx.media3.common.MediaItem.fromUri(newUrl)
                        exoPlayer.setMediaItem(mediaItem, resumePos)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                } else {
                    playerError = error.localizedMessage ?: "Ошибка воспроизведения видео"
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    hasAutoRetried = false
                    playerError = null
                }
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    if (viewModel.getNextEpisode() != null) {
                        viewModel.playNextEpisode()
                    }
                }
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                if (audioGroups.isNotEmpty()) {
                    val targetTitle = state.currentAudio?.title
                    var bestGroup: androidx.media3.common.Tracks.Group? = null
                    var bestTrackIndex = 0

                    if (!targetTitle.isNullOrBlank()) {
                        // 1. Try exact translation match first (distinguishes Дубляж vs Многоголосый for same studio)
                        for (group in audioGroups) {
                            val mg = group.mediaTrackGroup
                            for (i in 0 until mg.length) {
                                val format = mg.getFormat(i)
                                val label = format.label.orEmpty()
                                if (label.isNotBlank() && allohaTranslationNamesMatch(label, targetTitle, exactOnly = true)) {
                                    bestGroup = group
                                    bestTrackIndex = i
                                    break
                                }
                            }
                            if (bestGroup != null) break
                        }
                        // 2. Fallback to fuzzy match if exact match not found
                        if (bestGroup == null) {
                            for (group in audioGroups) {
                                val mg = group.mediaTrackGroup
                                for (i in 0 until mg.length) {
                                    val format = mg.getFormat(i)
                                    val label = format.label.orEmpty()
                                    if (label.isNotBlank() && allohaTranslationNamesMatch(label, targetTitle, exactOnly = false)) {
                                        bestGroup = group
                                        bestTrackIndex = i
                                        break
                                    }
                                }
                                if (bestGroup != null) break
                            }
                        }
                    }

                    if (bestGroup == null) {
                        for (group in audioGroups) {
                            val mg = group.mediaTrackGroup
                            for (i in 0 until mg.length) {
                                val format = mg.getFormat(i)
                                val lang = format.language?.lowercase(Locale.ROOT)
                                if (lang == "rus" || lang == "ru") {
                                    bestGroup = group
                                    bestTrackIndex = i
                                    break
                                }
                            }
                            if (bestGroup != null) break
                        }
                    }

                    if (bestGroup == null) {
                        bestGroup = audioGroups.firstOrNull()
                        bestTrackIndex = 0
                    }

                    val targetGroup = bestGroup
                    if (targetGroup != null && (!targetGroup.isSelected || !targetGroup.isTrackSelected(bestTrackIndex))) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setOverrideForType(androidx.media3.common.TrackSelectionOverride(targetGroup.mediaTrackGroup, listOf(bestTrackIndex)))
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            .build()
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Dynamic Audio track synchronization when selected translation changes in UI
    LaunchedEffect(state.currentAudio?.title) {
        val audioGroups = exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (audioGroups.isNotEmpty()) {
            val targetTitle = state.currentAudio?.title
            var bestGroup: androidx.media3.common.Tracks.Group? = null
            var bestTrackIndex = 0

            if (!targetTitle.isNullOrBlank()) {
                // 1. Try exact translation match first
                for (group in audioGroups) {
                    val mg = group.mediaTrackGroup
                    for (i in 0 until mg.length) {
                        val format = mg.getFormat(i)
                        val label = format.label.orEmpty()
                        if (label.isNotBlank() && allohaTranslationNamesMatch(label, targetTitle, exactOnly = true)) {
                            bestGroup = group
                            bestTrackIndex = i
                            break
                        }
                    }
                    if (bestGroup != null) break
                }
                // 2. Fallback to fuzzy match
                if (bestGroup == null) {
                    for (group in audioGroups) {
                        val mg = group.mediaTrackGroup
                        for (i in 0 until mg.length) {
                            val format = mg.getFormat(i)
                            val label = format.label.orEmpty()
                            if (label.isNotBlank() && allohaTranslationNamesMatch(label, targetTitle, exactOnly = false)) {
                                bestGroup = group
                                bestTrackIndex = i
                                break
                            }
                        }
                        if (bestGroup != null) break
                    }
                }
            }

            if (bestGroup == null) {
                for (group in audioGroups) {
                    val mg = group.mediaTrackGroup
                    for (i in 0 until mg.length) {
                        val format = mg.getFormat(i)
                        val lang = format.language?.lowercase(Locale.ROOT)
                        if (lang == "rus" || lang == "ru") {
                            bestGroup = group
                            bestTrackIndex = i
                            break
                        }
                    }
                    if (bestGroup != null) break
                }
            }

            if (bestGroup == null) {
                bestGroup = audioGroups.firstOrNull()
                bestTrackIndex = 0
            }

            val targetGroup = bestGroup
            if (targetGroup != null && (!targetGroup.isSelected || !targetGroup.isTrackSelected(bestTrackIndex))) {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .setOverrideForType(androidx.media3.common.TrackSelectionOverride(targetGroup.mediaTrackGroup, listOf(bestTrackIndex)))
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .build()
            }
        }
    }

    // Video quality switching via ExoPlayer native trackSelectionParameters (seamless 4K & adaptive)
    LaunchedEffect(state.currentQuality, exoPlayer.currentTracks) {
        val quality = state.currentQuality ?: return@LaunchedEffect
        if (quality.label == "Авто") {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(1920, 1088)
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .build()
            Log.d("PlayerScreen", "ExoPlayer video quality set to Auto (adaptive, capped at 1080p)")
        } else {
            val targetHeight = quality.label.removeSuffix("p").toIntOrNull()
                ?: if (quality.label.contains("4k", ignoreCase = true) || quality.label.contains("uhd", ignoreCase = true)) 2160 else null
            if (targetHeight != null) {
                val videoGroups = exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
                var matchedGroup: androidx.media3.common.Tracks.Group? = null
                var matchedTrackIndex = 0

                // Match track by height or widescreen dimensions (e.g. 1080p widescreen 1920x800, 720p widescreen 1280x534)
                for (group in videoGroups) {
                    val mg = group.mediaTrackGroup
                    for (i in 0 until mg.length) {
                        val format = mg.getFormat(i)
                        val h = format.height
                        val w = format.width
                        val is4k = targetHeight >= 2160 && (h >= 1400 || w >= 2560)
                        val is1080 = targetHeight in 1000..1200 && (h in 700..1200 || w in 1700..2100)
                        val is720 = targetHeight in 700..900 && (h in 480..800 || w in 1100..1500)
                        val is480 = targetHeight in 450..550 && (h in 320..550 || w in 600..900)
                        val is360 = targetHeight in 300..400 && (h in 200..400 || w in 350..550)
                        val isMatch = is4k || is1080 || is720 || is480 || is360 || (h in (targetHeight - 80)..(targetHeight + 80))
                        if (isMatch) {
                            matchedGroup = group
                            matchedTrackIndex = i
                            break
                        }
                    }
                    if (matchedGroup != null) break
                }

                if (matchedGroup != null) {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .clearVideoSizeConstraints()
                        .setOverrideForType(androidx.media3.common.TrackSelectionOverride(matchedGroup.mediaTrackGroup, listOf(matchedTrackIndex)))
                        .build()
                    Log.d("PlayerScreen", "ExoPlayer video quality overridden to track [group=${matchedGroup.mediaTrackGroup}, track=$matchedTrackIndex] for ${quality.label}")
                } else {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .clearVideoSizeConstraints()
                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        .build()
                    Log.d("PlayerScreen", "ExoPlayer video quality constraints cleared for dedicated stream ${quality.label}")
                }
            }
        }
    }

    // Media source loader (with exact position retention on stream change)
    LaunchedEffect(state.currentVideoUrl, state.currentSubtitle) {
        val videoUrl = state.currentVideoUrl ?: return@LaunchedEffect
        playerError = null

        val targetSeek = lastPreservedPositionMs ?: if (state.startPositionSec > 10) (state.startPositionSec * 1000).toLong() else 0L

        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            val streamUa = state.resolvedStream?.headers?.entries?.firstOrNull { it.key.equals("user-agent", ignoreCase = true) }?.value
            setUserAgent(streamUa ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
            setAllowCrossProtocolRedirects(true)
            setConnectTimeoutMs(20000)
            setReadTimeoutMs(20000)

            val customHeaders = mutableMapOf<String, String>()
            state.resolvedStream?.headers?.forEach { (k, v) ->
                val lower = k.lowercase()
                if (!lower.contains("origin") && !lower.startsWith("sec-") && !lower.contains("cookie")) {
                    customHeaders[k] = v
                }
            }

            val videoUri = runCatching { Uri.parse(videoUrl) }.getOrNull()
            val iframeUri = runCatching { Uri.parse(iframeUrl) }.getOrNull()
            val streamReferer = state.resolvedStream?.headers?.entries?.firstOrNull { it.key.equals("referer", ignoreCase = true) }?.value

            val finalReferer = when {
                !streamReferer.isNullOrBlank() && !streamReferer.contains("about:blank") && !streamReferer.contains("127.0.0.1") && !streamReferer.contains("localhost") -> streamReferer
                videoUri?.host != null && videoUri.host != "127.0.0.1" && videoUri.host != "localhost" -> "${videoUri.scheme ?: "https"}://${videoUri.host}/"
                iframeUri?.host != null && iframeUri.host != "127.0.0.1" && iframeUri.host != "localhost" -> "${iframeUri.scheme ?: "https"}://${iframeUri.host}/"
                else -> "https://alloha.tv/"
            }
            customHeaders["Referer"] = finalReferer

            try {
                val cookieManager = android.webkit.CookieManager.getInstance()
                val cookies = cookieManager.getCookie(videoUrl)
                    ?: cookieManager.getCookie(iframeUrl)
                    ?: (if (videoUri?.host != null) cookieManager.getCookie("${videoUri.scheme}://${videoUri.host}") else null)
                if (!cookies.isNullOrBlank() && !customHeaders.containsKey("Cookie") && !customHeaders.containsKey("cookie")) {
                    customHeaders["Cookie"] = cookies
                }
            } catch (e: Exception) {}

            setDefaultRequestProperties(customHeaders)
        }

        val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(videoUrl))
        val sub = state.currentSubtitle
        if (sub != null) {
            val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                .setMimeType(if (sub.url.contains(".vtt")) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP)
                .setLanguage(sub.language)
                .setLabel(sub.label)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
        }

        val isHls = videoUrl.contains(".m3u8", ignoreCase = true)
        val mediaSource = if (isHls) {
            androidx.media3.exoplayer.hls.HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(false)
                .createMediaSource(mediaItemBuilder.build())
        } else {
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(mediaItemBuilder.build())
        }
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        if (targetSeek > 0L) {
            exoPlayer.seekTo(targetSeek)
            if (lastPreservedPositionMs == null && state.startPositionSec > 10) {
                showResumeBadge = true
            }
        }
        lastPreservedPositionMs = null
        exoPlayer.playWhenReady = true
    }

    // Hide resume badge after 4 seconds
    LaunchedEffect(showResumeBadge) {
        if (showResumeBadge) {
            delay(4000)
            showResumeBadge = false
        }
    }

    // Save progress on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration, force = true)
            exoPlayer.release()
        }
    }

    // Handle Back button hierarchy
    BackHandler {
        when {
            showAudioDialog -> showAudioDialog = false
            showQualityDialog -> showQualityDialog = false
            showSubtitleDialog -> showSubtitleDialog = false
            activeError != null -> onBack()
            showControls -> showControls = false
            else -> onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode

                    if (!showControls && !isAnyModalOpen && activeError == null) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                if (showIntroPrompt && introRange != null) {
                                    introRange.let { exoPlayer.seekTo((it.end * 1000).toLong() + 500) }
                                    hasSkippedIntroForCurrentSession = true
                                    showIntroPrompt = false
                                    hudState = PlayerHudState.FORWARD
                                    true
                                } else {
                                    isPlaying = !isPlaying
                                    exoPlayer.playWhenReady = isPlaying
                                    hudState = if (isPlaying) PlayerHudState.PLAY else PlayerHudState.PAUSE
                                    true
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                                showControls = true
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                                hudState = PlayerHudState.REWIND
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                                hudState = PlayerHudState.FORWARD
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                isPlaying = !isPlaying
                                exoPlayer.playWhenReady = isPlaying
                                hudState = if (isPlaying) PlayerHudState.PLAY else PlayerHudState.PAUSE
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                isPlaying = true
                                exoPlayer.playWhenReady = true
                                hudState = PlayerHudState.PLAY
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                isPlaying = false
                                exoPlayer.playWhenReady = false
                                hudState = PlayerHudState.PAUSE
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                                hudState = PlayerHudState.FORWARD
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                                hudState = PlayerHudState.REWIND
                                true
                            }
                            else -> false
                        }
                    } else false
                } else false
            }
    ) {
        if (state.currentVideoUrl == null) {
            if (activeError != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(440.dp)
                            .clip(ContinuousRoundedRectangle(24.dp))
                            .background(GlassSurfaceDark)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), ContinuousRoundedRectangle(24.dp))
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Не удалось загрузить видео",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SlooshButton(
                                text = "Повторить",
                                isWhite = true,
                                onClick = {
                                    playerError = null
                                    viewModel.initPlayer(iframeUrl, mediaId, season, episode, displayTitle)
                                },
                                modifier = Modifier.focusRequester(errorFocusRequester)
                            )
                            SlooshButton(
                                text = "Назад",
                                onClick = onBack
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Загрузка видеопотока...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        } else {
            // ─── Video View Surface (Persistent when videoUrl is available) ───
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        keepScreenOn = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Stream transition overlay pill
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(ContinuousCapsule)
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), ContinuousCapsule)
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Переключение видеопотока...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Interactive error overlay (when playback error occurs during active playback)
            if (activeError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(440.dp)
                            .clip(ContinuousRoundedRectangle(24.dp))
                            .background(GlassSurfaceDark)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), ContinuousRoundedRectangle(24.dp))
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ошибка воспроизведения",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SlooshButton(
                                text = "Повторить",
                                isWhite = true,
                                onClick = {
                                    playerError = null
                                    hasAutoRetried = false
                                    val pos = currentPositionMs.takeIf { it > 0 }
                                        ?: lastPreservedPositionMs
                                        ?: (state.startPositionSec * 1000).toLong()
                                    lastPreservedPositionMs = pos
                                    if (state.errorMessage != null) {
                                        viewModel.initPlayer(iframeUrl, mediaId, season, episode, displayTitle, selectedVoice, directStreamUrl)
                                    } else {
                                        viewModel.retryPlayback(pos) { newUrl ->
                                            val mediaItem = androidx.media3.common.MediaItem.fromUri(newUrl)
                                            exoPlayer.setMediaItem(mediaItem, pos)
                                            exoPlayer.prepare()
                                            exoPlayer.playWhenReady = true
                                        }
                                    }
                                },
                                modifier = Modifier.focusRequester(errorFocusRequester)
                            )
                            SlooshButton(
                                text = "Назад",
                                onClick = onBack
                            )
                        }
                    }
                }
            }

            // ─── Resume Indicator Toast ───────────────────────────────
            AnimatedVisibility(
                visible = showResumeBadge,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 48.dp, top = 36.dp)
            ) {
                val sec = state.startPositionSec.toInt()
                val minStr = String.format("%02d:%02d", sec / 60, sec % 60)
                Box(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "▶ Продолжаем с $minStr",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ─── Center HUD (Clean Glass Icon Disc) ───────────────────
            AnimatedVisibility(
                visible = hudState != null && !showControls && !isAnyModalOpen,
                enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.82f),
                exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.82f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(ContinuousCapsule)
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (hudState) {
                        PlayerHudState.PLAY -> {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        PlayerHudState.PAUSE -> {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        PlayerHudState.REWIND -> {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        PlayerHudState.FORWARD -> {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        null -> {}
                    }
                }
            }

            // ─── Unobtrusive TV Prompt: Skip Intro while watching (Auto-fades after 7s) ───
            AnimatedVisibility(
                visible = showIntroPrompt && !showControls && !isAnyModalOpen,
                enter = fadeIn(tween(250)) + slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = fadeOut(tween(250)) + slideOutHorizontally(targetOffsetX = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp, bottom = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(GlassSurfaceDark.copy(alpha = 0.90f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), ContinuousCapsule)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(ContinuousCapsule)
                                .background(Color.White)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color.Black
                            )
                        }
                        Text(
                            text = "Пропустить заставку",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // ─── Top Bar & Title ──────────────────────────────────────
            AnimatedVisibility(
                visible = showControls && !isAnyModalOpen,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.92f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 48.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SlooshFocusableCard(
                                onClick = onBack,
                                shape = ContinuousCapsule,
                                modifier = Modifier
                                    .size(48.dp)
                                    .focusRequester(backFocusRequester)
                                    .focusProperties {
                                        canFocus = !isAnyModalOpen
                                        down = if (hasSkipAction) skipActionFocusRequester else seekbarFocusRequester
                                    }
                            ) { isFocused ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                                            ContinuousCapsule
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Назад",
                                        tint = if (isFocused) Color.Black else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            val curSeason = state.currentSeason ?: season
                            val curEp = state.currentEpisode ?: episode
                            if (curSeason != null && curEp != null) {
                                Text(
                                    text = "•  $curSeason сезон $curEp серия",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        // Badges (Pure Monochrome Glass)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.currentAudio?.let { audio ->
                                Box(
                                    modifier = Modifier
                                        .clip(ContinuousCapsule)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = audio.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                            state.currentQuality?.let { quality ->
                                Box(
                                    modifier = Modifier
                                        .clip(ContinuousCapsule)
                                        .background(Color.White.copy(alpha = 0.20f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = quality.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── Floating Next Episode Auto-Countdown Overlay ─────────────────────────
            val nextEp = viewModel.getNextEpisode()
            val remainingSec = if (durationMs > 0) ((durationMs - currentPositionMs) / 1000).toInt() else 0
            val showNextEpisodeBadge = nextEp != null && durationMs > 30_000L && remainingSec in 1..20 && !isAnyModalOpen && !showControls

            AnimatedVisibility(
                visible = showNextEpisodeBadge,
                enter = fadeIn(tween(250)) + slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = fadeOut(tween(250)) + slideOutHorizontally(targetOffsetX = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp, bottom = 48.dp)
            ) {
                if (nextEp != null) {
                    val (nextSeason, nextEpisodeObj) = nextEp
                    SlooshFocusableCard(
                        onClick = {
                            viewModel.playNextEpisode()
                        },
                        shape = ContinuousRoundedRectangle(18.dp),
                        modifier = Modifier.wrapContentSize()
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .clip(ContinuousRoundedRectangle(18.dp))
                                .background(if (isFocused) Color.White else GlassSurfaceDark)
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = if (isFocused) 0.5f else 0.15f),
                                    shape = ContinuousRoundedRectangle(18.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(ContinuousCapsule)
                                        .background(if (isFocused) Color.Black else Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = null,
                                        tint = if (isFocused) Color.White else Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Следующая серия через ${remainingSec}с",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isFocused) Color.Black.copy(alpha = 0.7f) else TextSecondaryDark
                                    )
                                    Text(
                                        text = "S${nextSeason} • E${nextEpisodeObj.episode}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = if (isFocused) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── Bottom Bar & Controls ────────────────────────────────
            AnimatedVisibility(
                visible = showControls && !isAnyModalOpen,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                            )
                        )
                        .padding(horizontal = 48.dp, vertical = 24.dp)
                ) {
                    Column {
                        // Timeline Time Labels & Integrated Skip Action Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val curSec = (currentPositionMs / 1000).toInt()
                            val durSec = (durationMs / 1000).toInt()
                            val curStr = if (curSec >= 3600)
                                String.format("%d:%02d:%02d", curSec / 3600, (curSec % 3600) / 60, curSec % 60)
                            else
                                String.format("%02d:%02d", curSec / 60, curSec % 60)
                            val durStr = if (durSec >= 3600)
                                String.format("%d:%02d:%02d", durSec / 3600, (durSec % 3600) / 60, durSec % 60)
                            else
                                String.format("%02d:%02d", durSec / 60, durSec % 60)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = curStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "/",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMutedDark
                                )
                                Text(
                                    text = durStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondaryDark
                                )
                            }

                            // Integrated Skip Action inside HUD (Never dangles)
                            if (isInIntro) {
                                SlooshFocusableCard(
                                    onClick = {
                                        introRange?.let { exoPlayer.seekTo((it.end * 1000).toLong() + 500) }
                                        hasSkippedIntroForCurrentSession = true
                                    },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier
                                        .height(34.dp)
                                        .focusRequester(skipActionFocusRequester)
                                        .focusProperties {
                                            canFocus = !isAnyModalOpen
                                            down = seekbarFocusRequester
                                            up = backFocusRequester
                                        }
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .background(
                                                if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                                                ContinuousCapsule
                                            )
                                            .border(
                                                1.dp,
                                                Color.White.copy(alpha = if (isFocused) 0.5f else 0.15f),
                                                ContinuousCapsule
                                            )
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FastForward,
                                                contentDescription = null,
                                                tint = if (isFocused) Color.Black else Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Пропустить заставку",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isFocused) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            } else if (isInOutro && outroRange != null) {
                                SlooshFocusableCard(
                                    onClick = {
                                        outroRange.let { exoPlayer.seekTo((it.end * 1000).toLong() + 500) }
                                    },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier
                                        .height(34.dp)
                                        .focusRequester(skipActionFocusRequester)
                                        .focusProperties {
                                            canFocus = !isAnyModalOpen
                                            down = seekbarFocusRequester
                                            up = backFocusRequester
                                        }
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .background(
                                                if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                                                ContinuousCapsule
                                            )
                                            .border(
                                                1.dp,
                                                Color.White.copy(alpha = if (isFocused) 0.5f else 0.15f),
                                                ContinuousCapsule
                                            )
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FastForward,
                                                contentDescription = null,
                                                tint = if (isFocused) Color.Black else Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Пропустить титры",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isFocused) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Interactive Focusable Timeline Seekbar (No card border!)
                        val seekInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        val isSeekbarFocused by seekInteractionSource.collectIsFocusedAsState()

                        val progressFraction = if (durationMs > 0)
                            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

                        val seekStepMs = remember(durationMs) {
                            if (durationMs > 0) (durationMs / 100).coerceIn(15_000L, 60_000L) else 20_000L
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .focusRequester(seekbarFocusRequester)
                                .focusProperties {
                                    canFocus = !isAnyModalOpen
                                    up = if (hasSkipAction) skipActionFocusRequester else backFocusRequester
                                    down = playFocusRequester
                                }
                                .focusable(interactionSource = seekInteractionSource)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                val target = (currentPositionMs - seekStepMs).coerceAtLeast(0L)
                                                exoPlayer.seekTo(target)
                                                currentPositionMs = target
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                val target = (currentPositionMs + seekStepMs).coerceAtMost(durationMs)
                                                exoPlayer.seekTo(target)
                                                currentPositionMs = target
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                                isPlaying = !isPlaying
                                                exoPlayer.playWhenReady = isPlaying
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val trackHeight = if (isSeekbarFocused) 6.dp else 4.dp
                            // Background Track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(trackHeight)
                                    .clip(ContinuousCapsule)
                                    .background(if (isSeekbarFocused) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.20f))
                            )
                            // Active Progress Fill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(trackHeight)
                                    .clip(ContinuousCapsule)
                                    .background(Color.White)
                            )
                            // Thumb indicator on focus
                            if (isSeekbarFocused) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .wrapContentWidth(Alignment.End)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(ContinuousCapsule)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Playback Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Prev Episode button
                            val hasPrev = viewModel.getPrevEpisode() != null
                            if (hasPrev) {
                                SlooshFocusableCard(
                                    onClick = {
                                        viewModel.playPrevEpisode()
                                        showControls = true
                                    },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .focusProperties {
                                            canFocus = !isAnyModalOpen
                                            up = seekbarFocusRequester
                                        }
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                                                ContinuousCapsule
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = "Предыдущая серия",
                                            tint = if (isFocused) Color.Black else Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(18.dp))
                            }

                            // Rewind -10s
                            SlooshFocusableCard(
                                onClick = {
                                    val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                    exoPlayer.seekTo(target)
                                    currentPositionMs = target
                                    showControls = true
                                },
                                shape = ContinuousCapsule,
                                modifier = Modifier
                                    .size(56.dp)
                                    .focusProperties {
                                        canFocus = !isAnyModalOpen
                                        up = seekbarFocusRequester
                                    }
                            ) { isFocused ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                                            ContinuousCapsule
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "-10с",
                                        tint = if (isFocused) Color.Black else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // Play / Pause (Primary Focus Button)
                            SlooshFocusableCard(
                                onClick = {
                                    isPlaying = !isPlaying
                                    exoPlayer.playWhenReady = isPlaying
                                    showControls = true
                                },
                                shape = ContinuousCapsule,
                                modifier = Modifier
                                    .size(72.dp)
                                    .focusRequester(playFocusRequester)
                                    .focusProperties {
                                        canFocus = !isAnyModalOpen
                                        up = seekbarFocusRequester
                                    }
                            ) { isFocused ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (isFocused) Color.White else Color.White.copy(alpha = 0.22f),
                                            ContinuousCapsule
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = if (isFocused) Color.Black else Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // Fast Forward +10s
                            SlooshFocusableCard(
                                onClick = {
                                    val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                    exoPlayer.seekTo(target)
                                    currentPositionMs = target
                                    showControls = true
                                },
                                shape = ContinuousCapsule,
                                modifier = Modifier
                                    .size(56.dp)
                                    .focusProperties {
                                        canFocus = !isAnyModalOpen
                                        up = seekbarFocusRequester
                                    }
                            ) { isFocused ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                                            ContinuousCapsule
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "+10с",
                                        tint = if (isFocused) Color.Black else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Next Episode button
                            val hasNext = viewModel.getNextEpisode() != null
                            if (hasNext) {
                                Spacer(modifier = Modifier.width(18.dp))
                                SlooshFocusableCard(
                                    onClick = {
                                        viewModel.playNextEpisode()
                                        showControls = true
                                    },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .focusProperties {
                                            canFocus = !isAnyModalOpen
                                            up = seekbarFocusRequester
                                        }
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                                                ContinuousCapsule
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Следующая серия",
                                            tint = if (isFocused) Color.Black else Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(36.dp))

                            // Audio Tracks Selector
                            val audios = state.resolvedStream?.audioVariants ?: emptyList()
                            if (audios.size > 1) {
                                SlooshFocusableCard(
                                    onClick = {
                                        showAudioDialog = true
                                    },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .focusProperties {
                                            canFocus = !isAnyModalOpen
                                            up = seekbarFocusRequester
                                        }
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                                                ContinuousCapsule
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RecordVoiceOver,
                                            contentDescription = "Озвучка",
                                            tint = if (isFocused) Color.Black else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                            }

                            // Subtitles Selector
                            val subs = state.resolvedStream?.subtitles ?: emptyList()
                            if (subs.isNotEmpty()) {
                                SlooshFocusableCard(
                                    onClick = {
                                        showSubtitleDialog = true
                                    },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .focusProperties {
                                            canFocus = !isAnyModalOpen
                                            up = seekbarFocusRequester
                                        }
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (isFocused) Color.White else if (state.currentSubtitle != null) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                                                ContinuousCapsule
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Subtitles,
                                            contentDescription = "Субтитры",
                                            tint = if (isFocused) Color.Black else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                            }

                            // Quality Selector
                            val qualities = state.resolvedStream?.qualityVariants ?: emptyList()
                            if (qualities.size > 1) {
                                SlooshFocusableCard(
                                    onClick = {
                                        showQualityDialog = true
                                    },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .focusProperties {
                                            canFocus = !isAnyModalOpen
                                            up = seekbarFocusRequester
                                        }
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                                                ContinuousCapsule
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = "Качество",
                                            tint = if (isFocused) Color.Black else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── Audio Dialog (Modal) ─────────────────────────────────
            AnimatedVisibility(
                visible = showAudioDialog,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(420.dp)
                            .clip(ContinuousRoundedRectangle(24.dp))
                            .background(GlassSurfaceDark)
                            .padding(28.dp)
                    ) {
                        Text(
                            text = "Выбор озвучки",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        val audios = remember(state.resolvedStream) {
                            state.resolvedStream?.audioVariants?.distinctBy { it.title } ?: emptyList()
                        }
                        TvLazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(audios.size, key = { "${audios[it].id}_$it" }) { idx ->
                                val audio = audios[idx]
                                val itemModifier = if (idx == 0) Modifier.focusRequester(modalFocusRequester) else Modifier
                                SlooshButton(
                                    text = com.sloosh.tv.ui.util.cleanTranslationName(audio.title),
                                    isWhite = state.currentAudio?.id == audio.id || state.currentAudio?.title == audio.title,
                                    onClick = {
                                        lastPreservedPositionMs = exoPlayer.currentPosition
                                        viewModel.selectAudioTrack(audio)
                                        showAudioDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth().then(itemModifier)
                                )
                            }
                        }
                    }
                }
            }

            // ─── Quality Dialog (Modal) ───────────────────────────────
            AnimatedVisibility(
                visible = showQualityDialog,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(400.dp)
                            .clip(ContinuousRoundedRectangle(24.dp))
                            .background(GlassSurfaceDark)
                            .padding(28.dp)
                    ) {
                        Text(
                            text = "Качество видео",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        val qualities = remember(state.resolvedStream) {
                            state.resolvedStream?.qualityVariants?.distinctBy { it.label } ?: emptyList()
                        }
                        TvLazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(qualities.size, key = { "${qualities[it].label}_$it" }) { idx ->
                                val quality = qualities[idx]
                                val itemModifier = if (idx == 0) Modifier.focusRequester(modalFocusRequester) else Modifier
                                SlooshButton(
                                    text = quality.label,
                                    isWhite = state.currentQuality?.label == quality.label,
                                    onClick = {
                                        lastPreservedPositionMs = exoPlayer.currentPosition
                                        viewModel.selectQuality(quality)
                                        showQualityDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth().then(itemModifier)
                                )
                            }
                        }
                    }
                }
            }

            // ─── Subtitles Dialog (Modal) ─────────────────────────────
            AnimatedVisibility(
                visible = showSubtitleDialog,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(400.dp)
                            .clip(ContinuousRoundedRectangle(24.dp))
                            .background(GlassSurfaceDark)
                            .padding(28.dp)
                    ) {
                        Text(
                            text = "Субтитры",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        val subs = remember(state.resolvedStream) {
                            state.resolvedStream?.subtitles?.distinctBy { it.url } ?: emptyList()
                        }
                        TvLazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(key = "sub_off") {
                                SlooshButton(
                                    text = "Выключить",
                                    isWhite = state.currentSubtitle == null,
                                    onClick = {
                                        lastPreservedPositionMs = exoPlayer.currentPosition
                                        viewModel.selectSubtitle(null)
                                        showSubtitleDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth().focusRequester(modalFocusRequester)
                                )
                            }
                            items(subs.size, key = { "${subs[it].url}_$it" }) { idx ->
                                val sub = subs[idx]
                                SlooshButton(
                                    text = sub.label,
                                    isWhite = state.currentSubtitle?.url == sub.url,
                                    onClick = {
                                        lastPreservedPositionMs = exoPlayer.currentPosition
                                        viewModel.selectSubtitle(sub)
                                        showSubtitleDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
