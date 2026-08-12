package com.sloosh.tv.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.sloosh.tv.ui.components.*
import com.sloosh.tv.ui.theme.BackgroundDark
import com.sloosh.tv.ui.theme.GlassSurfaceDark
import com.sloosh.tv.ui.theme.SlooshAccentDark

@Composable
fun DetailsScreen(
    mediaId: String,
    onPlayClick: (String, Int?, Int?) -> Unit,
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

    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SlooshAccentDark)
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Edge-to-Edge Backdrop Poster
        val backdropUrl = details.getDisplayBackdropUrl() ?: details.getDisplayPosterUrl()
        AsyncImage(
            model = backdropUrl,
            contentDescription = details.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 4-step Progressive Gradient Overlays
        ProgressiveGradientOverlay(
            direction = GradientDirection.HORIZONTAL_LEFT_TO_RIGHT,
            baseColor = Color.Black
        )
        ProgressiveGradientOverlay(
            direction = GradientDirection.VERTICAL_BOTTOM_TO_TOP,
            baseColor = Color.Black
        )

        // Details Panel
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 48.dp, top = 36.dp, end = 48.dp, bottom = 36.dp)
        ) {
            Text(
                text = details.title ?: details.originalTitle ?: "Без названия",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (details.ratings?.kp != null && details.ratings.kp > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlooshAccentDark)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", details.ratings.kp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Black
                        )
                    }
                }

                if (details.year != null) {
                    Text(
                        text = "${details.year} г.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                if (details.duration != null && details.duration > 0) {
                    Text(
                        text = "${details.duration} мин.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                if (!details.countries.isNullOrEmpty()) {
                    Text(
                        text = details.countries.joinToString(", "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Genres
            if (!details.genres.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(details.genres) { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassSurfaceDark)
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
            }

            // Description
            if (!details.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = details.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth(0.65f),
                    maxLines = 5
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val buttonText = if (state.progress != null && state.progress!!.positionSec > 10) {
                    "Продолжить просмотр"
                } else {
                    "Смотреть"
                }

                SlooshButton(
                    text = buttonText,
                    onClick = {
                        val iframe = state.allohaData?.movie?.iframeUrl ?: "https://alloha.tv/movie/${details.id}"
                        onPlayClick(iframe, state.selectedSeason, state.selectedEpisode)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    },
                    modifier = Modifier.focusRequester(watchButtonFocusRequester)
                )

                // Favorite Toggle Button
                SlooshFocusableCard(
                    onClick = { viewModel.toggleFavorite() },
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) { isFocused ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (state.isFavorite) SlooshAccentDark else Color.White
                        )
                    }
                }
            }
        }
    }
}
