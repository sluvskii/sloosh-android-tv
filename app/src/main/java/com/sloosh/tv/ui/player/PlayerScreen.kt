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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.media3.common.Player
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

    LaunchedEffect(iframeUrl) {
        viewModel.initPlayer(iframeUrl, mediaId)
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    // Position progress tracking loop
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            if (durationMs > 0) {
                viewModel.saveProgress(currentPositionMs, durationMs)
            }
            delay(2000)
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
        if (state.startPositionSec > 0) {
            exoPlayer.seekTo((state.startPositionSec * 1000).toLong())
        }
        exoPlayer.playWhenReady = true
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
                CircularProgressIndicator(color = SlooshAccentDark)
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

            // D-Pad Player Controls Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    // Progress Bar
                    val progressFraction = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
                    Slider(
                        value = progressFraction,
                        onValueChange = {},
                        enabled = false,
                        colors = SliderDefaults.colors(
                            disabledThumbColor = SlooshAccentDark,
                            disabledActiveTrackColor = SlooshAccentDark,
                            disabledInactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                            .width(320.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassSurfaceDark)
                            .padding(24.dp)
                    ) {
                        Text(text = "Выбор озвучки", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            .width(320.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassSurfaceDark)
                            .padding(24.dp)
                    ) {
                        Text(text = "Качество видео", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
