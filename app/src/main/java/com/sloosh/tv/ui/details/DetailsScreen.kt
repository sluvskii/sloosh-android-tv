package com.sloosh.tv.ui.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.sloosh.tv.data.api.MediaDetailsDto
import com.sloosh.tv.data.repository.AppSettings
import com.sloosh.tv.data.repository.DetailsScreenStyle
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.components.SlooshFocusableCard
import com.sloosh.tv.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
    mediaId: String,
    onPlayClick: (String, Int?, Int?, String) -> Unit,
    onBackClick: (() -> Unit)? = null,
    viewModel: DetailsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val watchButtonFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val appSettings = remember { AppSettings(context) }
    val detailsStyle = remember { appSettings.detailsStyle }

    LaunchedEffect(mediaId) {
        viewModel.loadDetails(mediaId)
    }

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            try { watchButtonFocusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    // ─── Loading ──────────────────────────────────────────────────────────────
    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SlooshGreen)
        }
        return
    }

    val details = state.details
    if (details == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Не удалось загрузить данные", color = Color.White)
        }
        return
    }

    val kpId = details.ids?.kp

    Box(modifier = modifier.fillMaxSize()) {
        if (detailsStyle == DetailsScreenStyle.SIDE_POSTER) {
            SidePosterDetailsLayout(
                details = details,
                state = state,
                viewModel = viewModel,
                watchButtonFocusRequester = watchButtonFocusRequester
            )
        } else {
            CenteredDetailsLayout(
                details = details,
                state = state,
                viewModel = viewModel,
                watchButtonFocusRequester = watchButtonFocusRequester,
                onBackClick = onBackClick
            )
        }

        // ─── Source Selection Sheet ───────────────────────────────────────────
        when {
            state.showSourceSheet && state.isFetchingSources -> {
                SourceSelectionLoadingDialog(
                    title = details.title ?: details.originalTitle ?: "",
                    onDismiss = { viewModel.dismissSourceSheet() }
                )
            }
            state.showSourceSheet && state.allohaData != null -> {
                val alloha = state.allohaData!!
                val savedVoiceover = kpId?.let { viewModel.allohaRepository.getLastVoiceover(it) }
                val globalLastVoiceover = viewModel.allohaRepository.getLastTranslation()
                val lastSeason = kpId?.let { viewModel.allohaRepository.getLastSeason(it) }
                val lastEpisode = kpId?.let { viewModel.allohaRepository.getLastEpisode(it) }

                SourceSelectionDialog(
                    allohaData = alloha,
                    kpId = kpId,
                    savedVoiceover = savedVoiceover,
                    globalLastVoiceover = globalLastVoiceover,
                    lastSeason = lastSeason ?: state.progress?.season,
                    lastEpisode = lastEpisode ?: state.progress?.episode,
                    onSelect = { result ->
                        // Save last-played preferences
                        if (kpId != null) {
                            viewModel.allohaRepository.saveLastVoiceover(kpId, result.translation.name)
                            viewModel.allohaRepository.saveLastPlayed(kpId, result.season, result.episode)
                        }
                        viewModel.allohaRepository.saveLastTranslation(result.translation.name)
                        viewModel.dismissSourceSheet()
                        onPlayClick(result.translation.iframeUrl, result.season, result.episode, details.displayTitle)
                    },
                    onDismiss = { viewModel.dismissSourceSheet() }
                )
            }
        }
    }
}

// ─── New Centered iOS Style Layout ─────────────────────────────────────────────

@Composable
private fun CenteredDetailsLayout(
    details: MediaDetailsDto,
    state: DetailsUiState,
    viewModel: DetailsViewModel,
    watchButtonFocusRequester: FocusRequester,
    onBackClick: (() -> Unit)?
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val backButtonFocusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen Backdrop Image
        val backdropUrl = details.getDisplayBackdropUrl() ?: details.getDisplayPosterUrl()
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Multi-Stop Vertical Gradient Overlay: Fades smoothly and deeply to solid BackgroundDark towards bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.40f),
                            0.15f to Color.Black.copy(alpha = 0.20f),
                            0.30f to Color.Black.copy(alpha = 0.55f),
                            0.48f to Color.Black.copy(alpha = 0.85f),
                            0.65f to BackgroundDark.copy(alpha = 0.98f),
                            0.80f to BackgroundDark,
                            1.0f to BackgroundDark
                        )
                    )
                )
        )

        // Soft Radial Vignette for centered text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        radius = 850f
                    )
                )
        )

        // Centered Content Column (shifted lower to showcase backdrop)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 760.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Generous top spacing to lower all elements down to the lower half
                Spacer(modifier = Modifier.height(180.dp))

                // 1. Logo or Title (Centered, refined size)
                val logoUrl = details.getDisplayLogoUrl()
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = details.title,
                        modifier = Modifier
                            .height(64.dp)
                            .widthIn(max = 280.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = details.title ?: details.originalTitle ?: "Без названия",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Metadata Row (Rating, Year, Duration, Country)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val kpRating = details.ratings?.kp
                    if (kpRating != null && kpRating > 0) {
                        Box(
                            modifier = Modifier
                                .clip(ContinuousRoundedRectangle(7.dp))
                                .background(ratingColor(kpRating))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", kpRating),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    if (details.year != null) {
                        Text(
                            text = "${details.year}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondaryDark
                        )
                    }

                    if (details.duration != null && details.duration > 0) {
                        val h = details.duration / 60
                        val m = details.duration % 60
                        val durText = if (h > 0) "${h} ч ${m} мин" else "${m} мин"
                        Text(
                            text = durText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondaryDark
                        )
                    }

                    if (!details.countries.isNullOrEmpty()) {
                        Text(
                            text = details.countries.take(2).joinToString(", "),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondaryDark
                        )
                    }
                }

                // 3. Genre Pills (Immediately under Rating/Year/Country)
                if (!details.genres.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        details.genres.take(5).forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(ContinuousCapsule)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // 4. Description (Pure text, NO container, centered on screen, left-aligned, above buttons)
                if (!details.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = details.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            ),
                            color = Color.White.copy(alpha = 0.78f),
                            textAlign = TextAlign.Start,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 5. Continue Progress Indicator (Under Description, before Play button)
                val prog = state.progress
                if (prog != null && prog.progressFraction > 0.01f) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val posSec = prog.positionSec.toInt()
                    val durSec = prog.durationSec.toInt()
                    val posStr = String.format("%02d:%02d", posSec / 60, posSec % 60)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Просмотрено $posStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlooshGreen
                        )
                        if (durSec > 0) {
                            val durStr = if (durSec >= 3600)
                                String.format("%d:%02d:%02d", durSec / 3600, (durSec % 3600) / 60, durSec % 60)
                            else
                                String.format("%02d:%02d", durSec / 60, durSec % 60)
                            Text(
                                text = " / $durStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { prog.progressFraction },
                        color = SlooshGreen,
                        trackColor = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .width(360.dp)
                            .height(3.5.dp)
                            .clip(ContinuousCapsule)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 6. Action Buttons Row (Play/Continue + Favorite Heart)
                val hasProgress = prog != null && prog.positionSec > 10
                val buttonText = if (hasProgress) {
                    val posStr = String.format("%02d:%02d", prog!!.positionSec.toInt() / 60, prog.positionSec.toInt() % 60)
                    "Продолжить с $posStr"
                } else "Смотреть"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SlooshButton(
                        text = buttonText,
                        onClick = { viewModel.openSourceSheet() },
                        isWhite = true,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier
                            .focusRequester(watchButtonFocusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                    keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                                ) {
                                    try {
                                        backButtonFocusRequester.requestFocus()
                                        true
                                    } catch (e: Exception) {
                                        false
                                    }
                                } else false
                            }
                    )

                    var favBounce by remember { mutableStateOf(false) }
                    val favScale by animateFloatAsState(
                        targetValue = if (favBounce) 1.4f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
                        finishedListener = { favBounce = false },
                        label = "favScale"
                    )
                    SlooshFocusableCard(
                        onClick = {
                            favBounce = true
                            viewModel.toggleFavorite()
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .size(52.dp)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                    keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                                ) {
                                    try {
                                        backButtonFocusRequester.requestFocus()
                                        true
                                    } catch (e: Exception) {
                                        false
                                    }
                                } else false
                            }
                    ) { _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (state.isFavorite) SlooshGreen.copy(alpha = 0.18f)
                                    else Color.White.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Избранное",
                                tint = if (state.isFavorite) SlooshGreen else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(favScale)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        // Top-Left Floating Glass Back Button (smoothly scrolls back to top when focused)
        if (onBackClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 36.dp, top = 36.dp)
            ) {
                SlooshFocusableCard(
                    onClick = onBackClick,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .focusRequester(backButtonFocusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(0)
                                }
                            }
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN
                            ) {
                                try {
                                    watchButtonFocusRequester.requestFocus()
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                            } else false
                        }
                ) { isFocused ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isFocused) Color.White.copy(alpha = 0.25f)
                                else Color.Black.copy(alpha = 0.40f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Classic Side-Poster Layout ───────────────────────────────────────────────

@Composable
private fun SidePosterDetailsLayout(
    details: MediaDetailsDto,
    state: DetailsUiState,
    viewModel: DetailsViewModel,
    watchButtonFocusRequester: FocusRequester
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val backdropUrl = details.getDisplayBackdropUrl() ?: details.getDisplayPosterUrl()
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.92f),
                            0.45f to Color.Black.copy(alpha = 0.70f),
                            0.65f to Color.Transparent
                        )
                    )
                )
        )

        val posterUrl = details.getDisplayPosterUrl()
        if (posterUrl != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 56.dp)
                    .fillMaxHeight(0.75f)
                    .aspectRatio(2f / 3f)
                    .clip(ContinuousRoundedRectangle(20.dp))
            ) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = details.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.62f)
                .verticalScroll(rememberScrollState())
                .padding(start = 80.dp, top = 48.dp, end = 32.dp, bottom = 48.dp)
        ) {
            val logoUrl = details.getDisplayLogoUrl()
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = details.title,
                    modifier = Modifier
                        .height(80.dp)
                        .widthIn(max = 320.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Text(
                    text = details.title ?: details.originalTitle ?: "Без названия",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 42.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val kpRating = details.ratings?.kp
                if (kpRating != null && kpRating > 0) {
                    Box(
                        modifier = Modifier
                            .clip(ContinuousRoundedRectangle(7.dp))
                            .background(ratingColor(kpRating))
                            .padding(horizontal = 6.5.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", kpRating),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            color = Color.White
                        )
                    }
                }

                if (details.year != null) {
                    Text(
                        text = "${details.year}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondaryDark
                    )
                }

                if (details.duration != null && details.duration > 0) {
                    val h = details.duration / 60
                    val m = details.duration % 60
                    val durText = if (h > 0) "${h} ч ${m} мин" else "${m} мин"
                    Text(
                        text = durText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondaryDark
                    )
                }

                if (!details.countries.isNullOrEmpty()) {
                    Text(
                        text = details.countries.take(2).joinToString(", "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!details.genres.isNullOrEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(details.genres) { genre ->
                        Box(
                            modifier = Modifier
                                .clip(ContinuousCapsule)
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!details.description.isNullOrEmpty()) {
                Text(
                    text = details.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    lineHeight = 22.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            val prog = state.progress
            if (prog != null && prog.progressFraction > 0.01f) {
                val posSec = prog.positionSec.toInt()
                val durSec = prog.durationSec.toInt()
                val posStr = String.format("%02d:%02d", posSec / 60, posSec % 60)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Просмотрено $posStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlooshGreen
                    )
                    if (durSec > 0) {
                        val durStr = if (durSec >= 3600)
                            String.format("%d:%02d:%02d", durSec / 3600, (durSec % 3600) / 60, durSec % 60)
                        else
                            String.format("%02d:%02d", durSec / 60, durSec % 60)
                        Text(
                            text = " / $durStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedDark
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { prog.progressFraction },
                    color = SlooshGreen,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(3.dp)
                        .clip(ContinuousCapsule)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val hasProgress = prog != null && prog.positionSec > 10
                val buttonText = if (hasProgress) {
                    val posStr = String.format("%02d:%02d", prog!!.positionSec.toInt() / 60, prog.positionSec.toInt() % 60)
                    "Продолжить с $posStr"
                } else "Смотреть"

                SlooshButton(
                    text = buttonText,
                    onClick = { viewModel.openSourceSheet() },
                    isWhite = true,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.focusRequester(watchButtonFocusRequester)
                )

                var favBounce by remember { mutableStateOf(false) }
                val favScale by animateFloatAsState(
                    targetValue = if (favBounce) 1.4f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
                    finishedListener = { favBounce = false },
                    label = "favScale"
                )
                SlooshFocusableCard(
                    onClick = {
                        favBounce = true
                        viewModel.toggleFavorite()
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) { _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (state.isFavorite) SlooshGreen.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (state.isFavorite) SlooshGreen else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(24.dp)
                                .scale(favScale)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

