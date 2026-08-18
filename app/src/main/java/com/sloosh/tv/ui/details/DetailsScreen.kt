package com.sloosh.tv.ui.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.components.SlooshFocusableCard
import com.sloosh.tv.ui.theme.*
import com.sloosh.tv.ui.util.rememberAdaptiveAmbientColor
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
            CircularProgressIndicator(color = Color.White)
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
        SidePosterDetailsLayout(
            details = details,
            state = state,
            viewModel = viewModel,
            watchButtonFocusRequester = watchButtonFocusRequester,
            onPlayClick = onPlayClick,
            onBackClick = onBackClick
        )

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

// ─── Details Layout With Side Poster ──────────────────────────────────────────

@Composable
private fun SidePosterDetailsLayout(
    details: MediaDetailsDto,
    state: DetailsUiState,
    viewModel: DetailsViewModel,
    watchButtonFocusRequester: FocusRequester,
    onPlayClick: (String, Int?, Int?, String) -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val backButtonFocusRequester = remember { FocusRequester() }
    val moreButtonFocusRequester = remember { FocusRequester() }
    var isExpanded by remember { mutableStateOf(false) }
    var canExpand by remember(details.description) { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val posterUrl = details.getDisplayPosterUrl()
    val previewBackdropUrl = details.getPreviewBackdropUrl()
    val backdropUrl = details.getDisplayBackdropUrl() ?: posterUrl
    val ambientColor by rememberAdaptiveAmbientColor(
        primaryUrl = previewBackdropUrl ?: backdropUrl,
        fallbackUrl = posterUrl
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientColor)
    ) {
        // Subtle depth gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to ambientColor,
                            0.25f to ambientColor.copy(alpha = 0.90f),
                            0.45f to ambientColor.copy(alpha = 0.60f),
                            0.65f to ambientColor.copy(alpha = 0.25f),
                            0.85f to Color.Transparent
                        )
                    )
                )
        )

        // Native aspect-ratio backdrop shifted to the right, with ultra-smooth alpha fade on its left edge
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(16f / 9f)
                    .offset(x = 155.dp)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.15f to Color.Black.copy(alpha = 0.03f),
                                    0.30f to Color.Black.copy(alpha = 0.12f),
                                    0.45f to Color.Black.copy(alpha = 0.30f),
                                    0.60f to Color.Black.copy(alpha = 0.55f),
                                    0.75f to Color.Black.copy(alpha = 0.80f),
                                    0.90f to Color.Black.copy(alpha = 0.96f),
                                    1.0f to Color.Black
                                ),
                                startX = 0f,
                                endX = size.width * 0.72f
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                contentScale = ContentScale.FillHeight
            )
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.56f)
                .verticalScroll(scrollState)
                .padding(start = 56.dp, top = 36.dp, end = 24.dp, bottom = 48.dp)
        ) {
            // ─── Top Back Button ──────────────────────────────────────────────
            if (onBackClick != null) {
                SlooshFocusableCard(
                    onClick = onBackClick,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(44.dp)
                        .focusRequester(backButtonFocusRequester)
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
                                if (isFocused) Color.White.copy(alpha = 0.30f)
                                else Color.White.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ─── Logo or Title ────────────────────────────────────────────────
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
                    lineHeight = 42.sp,
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            RatingsRow(
                kp = details.ratings?.kp,
                tmdb = details.ratings?.tmdb,
                imdb = details.ratings?.imdb ?: details.ratings?.kp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        durText,
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

            // Description
            if (!details.description.isNullOrEmpty()) {
                val desc = details.description

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                ) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 19.5.sp
                        ),
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = TextAlign.Start,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textLayoutResult ->
                            if (!isExpanded) {
                                canExpand = textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > 4
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (canExpand) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SlooshFocusableCard(
                            onClick = { isExpanded = !isExpanded },
                            shape = ContinuousCapsule,
                            modifier = Modifier
                                .wrapContentSize()
                                .focusRequester(moreButtonFocusRequester)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                try {
                                                    if (onBackClick != null) {
                                                        backButtonFocusRequester.requestFocus()
                                                        true
                                                    } else false
                                                } catch (e: Exception) {
                                                    false
                                                }
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                try {
                                                    watchButtonFocusRequester.requestFocus()
                                                    true
                                                } catch (e: Exception) {
                                                    false
                                                }
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                        ) { isFocused ->
                            Box(
                                modifier = Modifier
                                    .clip(ContinuousCapsule)
                                    .background(
                                        if (isFocused) Color.White.copy(alpha = 0.28f)
                                        else Color.White.copy(alpha = 0.12f)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isExpanded) "Свернуть" else "Ещё",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.sp
                                    ),
                                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
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
                        color = Color.White.copy(alpha = 0.90f)
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
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.18f),
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
                    modifier = Modifier
                        .focusRequester(watchButtonFocusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                            ) {
                                try {
                                    if (canExpand) {
                                        moreButtonFocusRequester.requestFocus()
                                        true
                                    } else if (onBackClick != null) {
                                        backButtonFocusRequester.requestFocus()
                                        true
                                    } else false
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
                                    if (canExpand) {
                                        moreButtonFocusRequester.requestFocus()
                                        true
                                    } else if (onBackClick != null) {
                                        backButtonFocusRequester.requestFocus()
                                        true
                                    } else false
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
                                if (state.isFavorite) Color.White.copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (state.isFavorite) Color.White else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(24.dp)
                                .scale(favScale)
                        )
                    }
                }
            }

            // ─── TV Series Seasons & Episodes Section ─────────────────────────
            val seriesData = state.allohaData
            if (seriesData != null && seriesData.isSerial && seriesData.seasons.isNotEmpty()) {
                val seasons = seriesData.seasons
                var selectedSeasonIndex by remember { mutableStateOf(0) }
                val currentSeason = seasons.getOrNull(selectedSeasonIndex) ?: seasons.firstOrNull()

                if (currentSeason != null) {
                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Сезоны и серии",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Seasons Tabs Row
                    if (seasons.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 14.dp)
                        ) {
                            items(seasons) { s ->
                                val sIdx = seasons.indexOf(s)
                                val isSelected = selectedSeasonIndex == sIdx

                                SlooshFocusableCard(
                                    onClick = { selectedSeasonIndex = sIdx },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier.wrapContentSize()
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .clip(ContinuousCapsule)
                                            .background(
                                                if (isSelected && isFocused) Color.White
                                                else if (isSelected) Color.White.copy(alpha = 0.90f)
                                                else if (isFocused) Color.White.copy(alpha = 0.25f)
                                                else Color.White.copy(alpha = 0.08f)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Сезон ${s.season}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Episodes Horizontal Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp, end = 24.dp)
                    ) {
                        items(currentSeason.episodes) { ep ->
                            val validId = details.id?.replace("kp_", "") ?: ""
                            val episodeStillUrl = "https://api.neome.uk/api/v1/images/screens/$validId/${currentSeason.season}/${ep.episode}/large"

                            val isCurrentWatching = state.progress?.season == currentSeason.season && state.progress?.episode == ep.episode

                            DetailsEpisodeCard(
                                season = currentSeason.season,
                                episode = ep.episode,
                                stillUrl = episodeStillUrl,
                                fallbackArtworkUrl = backdropUrl,
                                isCurrentWatching = isCurrentWatching,
                                progressFraction = if (isCurrentWatching) state.progress?.progressFraction ?: 0f else 0f,
                                onClick = {
                                    val firstTranslation = ep.translations.firstOrNull()
                                    if (firstTranslation != null) {
                                        onPlayClick(
                                            firstTranslation.iframeUrl,
                                            currentSeason.season,
                                            ep.episode,
                                            details.displayTitle
                                        )
                                    } else {
                                        viewModel.openSourceSheet()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun RatingsRow(
    kp: Double?,
    tmdb: Double?,
    imdb: Double?,
    modifier: Modifier = Modifier
) {
    val hasKp = kp != null && kp > 0
    val hasTmdb = tmdb != null && tmdb > 0
    val hasImdb = imdb != null && imdb > 0

    if (!hasKp && !hasTmdb && !hasImdb) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasKp) {
            RatingBadge(
                logoBg = Color(0xFFFF6A00),
                logoText = "KP",
                rating = kp!!
            )
        }
        if (hasTmdb) {
            RatingBadge(
                logoBg = Color(0xFF01D277),
                logoText = "TMDB",
                rating = tmdb!!
            )
        }
        if (hasImdb) {
            RatingBadge(
                logoBg = Color(0xFFF5C518),
                logoText = "IMDb",
                rating = imdb!!,
                textColor = Color.Black
            )
        }
    }
}

@Composable
private fun RatingBadge(
    logoBg: Color,
    logoText: String,
    rating: Double,
    textColor: Color = Color.White
) {
    Box(
        modifier = Modifier
            .clip(ContinuousCapsule)
            .background(Color.White.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = ContinuousCapsule
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(ContinuousRoundedRectangle(4.dp))
                    .background(logoBg)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = logoText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )
            }
            Text(
                text = String.format(java.util.Locale.US, "%.1f", rating),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun DetailsEpisodeCard(
    season: Int,
    episode: Int,
    stillUrl: String,
    fallbackArtworkUrl: String?,
    isCurrentWatching: Boolean,
    progressFraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SlooshFocusableCard(
        onClick = onClick,
        modifier = modifier
            .width(220.dp)
            .height(124.dp),
        shape = ContinuousRoundedRectangle(14.dp)
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ContinuousRoundedRectangle(14.dp))
        ) {
            AsyncImage(
                model = stillUrl.ifEmpty { fallbackArtworkUrl },
                contentDescription = "Серия $episode",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.20f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            // Play indicator on focus
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                        .clip(ContinuousCapsule)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Смотреть",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Info Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Серия $episode",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White,
                    maxLines = 1
                )

                if (isCurrentWatching && progressFraction > 0.01f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(ContinuousCapsule)
                    )
                }
            }
        }
    }
}



