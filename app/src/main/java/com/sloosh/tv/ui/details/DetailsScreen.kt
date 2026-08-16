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
        SidePosterDetailsLayout(
            details = details,
            state = state,
            viewModel = viewModel,
            watchButtonFocusRequester = watchButtonFocusRequester,
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
    onBackClick: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val backButtonFocusRequester = remember { FocusRequester() }
    val moreButtonFocusRequester = remember { FocusRequester() }
    var isExpanded by remember { mutableStateOf(false) }

    val backdropUrl = details.getDisplayBackdropUrl() ?: details.getDisplayPosterUrl()
    val ambientColor by rememberAdaptiveAmbientColor(imageUrl = backdropUrl)

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
                            0.35f to ambientColor.copy(alpha = 0.85f),
                            0.65f to Color.Transparent
                        )
                    )
                )
        )

        // Native aspect-ratio backdrop shifted to the right, with genuine alpha fade on its left edge
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
                                    0.22f to Color.Black.copy(alpha = 0.08f),
                                    0.42f to Color.Black.copy(alpha = 0.35f),
                                    0.68f to Color.Black.copy(alpha = 0.75f),
                                    0.88f to Color.Black.copy(alpha = 0.95f),
                                    1.0f to Color.Black
                                ),
                                startX = 0f,
                                endX = size.width * 0.58f
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

            // Description (4 lines max with true alpha fade on line 4)
            if (!details.description.isNullOrEmpty()) {
                val desc = details.description
                val canExpand = desc.length > 180

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (!isExpanded) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.72f),
                                textAlign = TextAlign.Start,
                                maxLines = 4,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (canExpand) {
                                            Modifier
                                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                                .drawWithContent {
                                                    drawContent()
                                                    val fadeWidth = 65.dp.toPx()
                                                    val fadeHeight = 22.dp.toPx()
                                                    val buttonWidth = 52.dp.toPx()
                                                    drawRect(
                                                        brush = Brush.horizontalGradient(
                                                            colors = listOf(Color.Black, Color.Transparent),
                                                            startX = size.width - fadeWidth - buttonWidth,
                                                            endX = size.width - buttonWidth + 8.dp.toPx()
                                                        ),
                                                        topLeft = Offset(size.width - fadeWidth - buttonWidth, size.height - fadeHeight),
                                                        size = Size(fadeWidth + buttonWidth, fadeHeight),
                                                        blendMode = BlendMode.DstIn
                                                    )
                                                }
                                        } else Modifier
                                    )
                            )

                            if (canExpand) {
                                SlooshFocusableCard(
                                    onClick = { isExpanded = true },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
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
                                            .background(
                                                if (isFocused) Color.White.copy(alpha = 0.30f)
                                                else Color.White.copy(alpha = 0.14f)
                                            )
                                            .padding(horizontal = 9.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ЕЩЁ",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                letterSpacing = 0.6.sp
                                            ),
                                            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.90f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.72f),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            SlooshFocusableCard(
                                onClick = { isExpanded = false },
                                shape = ContinuousCapsule,
                                modifier = Modifier
                                    .align(Alignment.End)
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
                                        .background(
                                            if (isFocused) Color.White.copy(alpha = 0.30f)
                                            else Color.White.copy(alpha = 0.14f)
                                        )
                                        .padding(horizontal = 9.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "СВЕРНУТЬ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            letterSpacing = 0.6.sp
                                        ),
                                        color = if (isFocused) Color.White else Color.White.copy(alpha = 0.90f)
                                    )
                                }
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
                    modifier = Modifier
                        .focusRequester(watchButtonFocusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                            ) {
                                try {
                                    if (!details.description.isNullOrEmpty() && details.description.length > 180) {
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
                                    if (!details.description.isNullOrEmpty() && details.description.length > 180) {
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

