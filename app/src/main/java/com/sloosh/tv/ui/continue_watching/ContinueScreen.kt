package com.sloosh.tv.ui.continue_watching

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.components.SlooshFocusableCard
import com.sloosh.tv.ui.theme.*

@Composable
fun ContinueScreen(
    onMediaSelected: (String) -> Unit,
    viewModel: ContinueViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedItemForAction by remember { mutableStateOf<ContinueWatchingItem?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(start = 4.dp, top = 32.dp, end = 24.dp, bottom = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Text(
                text = "Продолжить",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (state.isLoading && state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                }
            } else if (state.items.isEmpty()) {
                // Empty State (Matches iOS ContinueEmptyState)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(ContinuousCapsule)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "История просмотров пуста",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Фильмы и сериалы, которые вы начнете смотреть, появятся здесь для быстрого продолжения.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.width(420.dp)
                        )
                    }
                }
            } else {
                // Grid of Continue Watching Cards (Wide 16:9 cards)
                TvLazyVerticalGrid(
                    columns = TvGridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 40.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = { onMediaSelected(item.mediaId) }
                        )
                    }
                }
            }
        }

        // Action / Details Dialog for an Item
        AnimatedVisibility(
            visible = selectedItemForAction != null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            val item = selectedItemForAction
            if (item != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
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
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${item.timeLabel} • ${item.remainingLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SlooshButton(
                                text = "Продолжить просмотр",
                                isPrimary = true,
                                onClick = {
                                    selectedItemForAction = null
                                    onMediaSelected(item.mediaId)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            SlooshButton(
                                text = "Отметить просмотренным",
                                onClick = {
                                    viewModel.markAsWatched(item)
                                    selectedItemForAction = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            SlooshButton(
                                text = "Удалить из истории",
                                onClick = {
                                    viewModel.removeFromHistory(item)
                                    selectedItemForAction = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            SlooshButton(
                                text = "Закрыть",
                                onClick = { selectedItemForAction = null },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SlooshFocusableCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = ContinuousRoundedRectangle(18.dp)
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ContinuousRoundedRectangle(18.dp))
        ) {
            val artworkUrl = item.backdropUrl ?: item.posterUrl
            AsyncImage(
                model = artworkUrl,
                contentDescription = item.title,
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
                                0.25f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.94f)
                            )
                        )
                    )
            )

            // Play indicator badge on focus
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(54.dp)
                        .clip(ContinuousCapsule)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Смотреть",
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Info Content at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (item.isEpisode && item.season != null && item.episode != null) {
                    Text(
                        text = "Сезон ${item.season} • Серия ${item.episode}  •  ${item.remainingLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "${item.timeLabel}  •  ${item.remainingLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // White Progress Bar
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(ContinuousCapsule)
                )
            }
        }
    }
}
