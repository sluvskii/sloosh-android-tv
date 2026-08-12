package com.sloosh.tv.ui.player

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.components.SlooshFocusableCard
import com.sloosh.tv.ui.theme.BackgroundDark
import com.sloosh.tv.ui.theme.GlassSurfaceDark
import com.sloosh.tv.ui.theme.SlooshAccentDark
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    iframeUrl: String,
    mediaId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var showAudioDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showResumeBadge by remember { mutableStateOf(false) }

    LaunchedEffect(iframeUrl) {
        viewModel.initPlayer(iframeUrl, mediaId)
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    // Track position & save progress
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            if (durationMs > 0) {
                viewModel.saveProgress(currentPositionMs, durationMs)
            }
            delay(1500)
        }
    }

    // Media Source loading when stream URL changes
    LaunchedEffect(state.currentVideoUrl) {
        val videoUrl = state.currentVideoUrl ?: return@LaunchedEffect
        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            val headers = state.resolvedStream?.headers ?: emptyMap()
            setDefaultRequestProperties(headers)
        }

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)))

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        if (state.startPositionSec > 10) {
            exoPlayer.seekTo((state.startPositionSec * 1000).toLong())
            showResumeBadge = true
        }
        exoPlayer.playWhenReady = true
    }

    // Hide resume badge after 4 seconds
    LaunchedEffect(showResumeBadge) {
        if (showResumeBadge) {
            delay(4000)
            showResumeBadge = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SlooshAccentDark)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Подключение к Alloha HLS...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // AndroidX Media3 Player Surface View
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // Custom Sloosh remote controller
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Resume indicator toast
            AnimatedVisibility(
                visible = showResumeBadge,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
            ) {
                val sec = state.startPositionSec.toInt()
                val minStr = String.format("%02d:%02d", sec / 60, sec % 60)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassSurfaceDark)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Продолжаем с $minStr",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlooshAccentDark
                    )
                }
            }

            // Skip Intro / Outro Floating Button
            val introRange = state.resolvedStream?.introRange
            val posSec = currentPositionMs / 1000.0
            val showSkipIntro = introRange != null && posSec >= introRange.start && posSec <= introRange.end

            if (showSkipIntro && introRange != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 120.dp, end = 48.dp)
                ) {
                    SlooshButton(
                        text = "Пропустить заставку",
                        isPrimary = true,
                        onClick = {
                            exoPlayer.seekTo((introRange.end * 1000).toLong())
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                    )
                }
            }

            // D-Pad Player Controls Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    // Time Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentSec = (currentPositionMs / 1000).toInt()
                        val totalSec = (durationMs / 1000).toInt()
                        Text(
                            text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        Text(
                            text = String.format("%02d:%02d", totalSec / 60, totalSec % 60),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Bar
                    val progressFraction = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = SlooshAccentDark,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Controller Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind -10s
                        SlooshFocusableCard(
                            onClick = {
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.FastRewind, contentDescription = "-10s", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Play/Pause
                        SlooshFocusableCard(
                            onClick = {
                                isPlaying = !isPlaying
                                exoPlayer.playWhenReady = isPlaying
                            },
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Fast Forward +10s
                        SlooshFocusableCard(
                            onClick = {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        // Audio Track Selector
                        if ((state.resolvedStream?.audioVariants?.size ?: 0) > 1) {
                            SlooshFocusableCard(
                                onClick = { showAudioDialog = true },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Озвучка", tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        // Quality Selector
                        if ((state.resolvedStream?.qualityVariants?.size ?: 0) > 1) {
                            SlooshFocusableCard(
                                onClick = { showQualityDialog = true },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Качество", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Audio Dialog
            AnimatedVisibility(visible = showAudioDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(340.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(GlassSurfaceDark)
                            .padding(24.dp)
                    ) {
                        Text(text = "Выбор озвучки", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.resolvedStream?.audioVariants ?: emptyList()) { audio ->
                                SlooshButton(
                                    text = audio.title,
                                    isPrimary = state.currentAudio?.id == audio.id,
                                    onClick = {
                                        viewModel.selectAudioTrack(audio)
                                        showAudioDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Quality Dialog
            AnimatedVisibility(visible = showQualityDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(340.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(GlassSurfaceDark)
                            .padding(24.dp)
                    ) {
                        Text(text = "Качество видео", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.resolvedStream?.qualityVariants ?: emptyList()) { quality ->
                                SlooshButton(
                                    text = quality.label,
                                    isPrimary = state.currentQuality?.label == quality.label,
                                    onClick = {
                                        viewModel.selectQuality(quality)
                                        showQualityDialog = false
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
