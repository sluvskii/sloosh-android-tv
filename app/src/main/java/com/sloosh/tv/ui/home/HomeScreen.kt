package com.sloosh.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.ui.components.*
import com.sloosh.tv.ui.theme.BackgroundDark
import com.sloosh.tv.ui.theme.SlooshAccentDark

@Composable
fun HomeScreen(
    onMediaSelected: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(start = 32.dp, top = 24.dp, end = 32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    ShimmerEffect(modifier = Modifier.fillMaxSize())
                }
                CatalogRowShimmer("Популярное")
                CatalogRowShimmer("Топ фильмов")
            }
        }
        return
    }

    val hero = state.heroItem

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Hero Edge-to-Edge Backdrop
        if (hero != null) {
            val backdropUrl = hero.getDisplayPosterUrl()
            AsyncImage(
                model = backdropUrl,
                contentDescription = hero.displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Progressive Gradient Overlay (Horizontal + Vertical)
            ProgressiveGradientOverlay(
                direction = GradientDirection.HORIZONTAL_LEFT_TO_RIGHT,
                baseColor = Color.Black
            )
            ProgressiveGradientOverlay(
                direction = GradientDirection.VERTICAL_BOTTOM_TO_TOP,
                baseColor = Color.Black
            )
        }

        // Screen Content Scroll
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, top = 24.dp, end = 32.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Hero Banner Info
            item {
                if (hero != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .padding(top = 20.dp)
                    ) {
                        Text(
                            text = hero.displayTitle,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (hero.rating != null && hero.rating > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlooshAccentDark)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", hero.rating),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black
                                    )
                                }
                            }

                            if (hero.yearString.isNotEmpty()) {
                                Text(
                                    text = hero.yearString,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (!hero.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = hero.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 3
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        SlooshButton(
                            text = "Смотреть",
                            onClick = { onMediaSelected(hero.identifier) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                            }
                        )
                    }
                }
            }

            // Continue Watching Row
            if (state.continueWatching.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Продолжить просмотр",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(end = 32.dp)
                        ) {
                            items(state.continueWatching, key = { it.mediaId }) { progress ->
                                ContinueWatchingCard(
                                    progress = progress,
                                    onClick = { onMediaSelected(progress.mediaId) }
                                )
                            }
                        }
                    }
                }
            }

            // Category Rows
            if (state.popularMovies.isNotEmpty()) {
                item {
                    CategoryRow(
                        title = "Популярное",
                        items = state.popularMovies,
                        onItemFocused = { viewModel.selectHeroItem(it) },
                        onItemClick = { onMediaSelected(it.identifier) }
                    )
                }
            }

            if (state.topMovies.isNotEmpty()) {
                item {
                    CategoryRow(
                        title = "Топ фильмов",
                        items = state.topMovies,
                        onItemFocused = { viewModel.selectHeroItem(it) },
                        onItemClick = { onMediaSelected(it.identifier) }
                    )
                }
            }

            if (state.topTv.isNotEmpty()) {
                item {
                    CategoryRow(
                        title = "Топ сериалов",
                        items = state.topTv,
                        onItemFocused = { viewModel.selectHeroItem(it) },
                        onItemClick = { onMediaSelected(it.identifier) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    progress: ProgressEntity,
    onClick: () -> Unit
) {
    SlooshFocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(130.dp),
        shape = RoundedCornerShape(14.dp)
    ) { isFocused ->
        Box(modifier = Modifier.fillMaxSize()) {
            val backdropUrl = progress.backdropUrl ?: progress.posterUrl
            AsyncImage(
                model = backdropUrl,
                contentDescription = progress.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = if (progress.title.isNotEmpty()) progress.title else "Просмотр",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1
                )

                if (progress.isEpisode) {
                    Text(
                        text = "S${progress.season} E${progress.episode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlooshAccentDark
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress.progressFraction },
                    color = SlooshAccentDark,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
fun CategoryRow(
    title: String,
    items: List<MediaDto>,
    onItemFocused: (MediaDto) -> Unit,
    onItemClick: (MediaDto) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 32.dp)
        ) {
            items(items, key = { it.identifier }) { item ->
                MediaCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onFocus = { onItemFocused(item) }
                )
            }
        }
    }
}

@Composable
fun MediaCard(
    item: MediaDto,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    SlooshFocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(210.dp),
        shape = RoundedCornerShape(14.dp)
    ) { isFocused ->
        if (isFocused) {
            onFocus()
        }
        AsyncImage(
            model = item.getDisplayPosterUrl(),
            contentDescription = item.displayTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
