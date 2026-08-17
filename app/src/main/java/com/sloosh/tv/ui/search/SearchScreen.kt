package com.sloosh.tv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.components.SlooshFocusableCard
import com.sloosh.tv.ui.home.MediaCard
import com.sloosh.tv.ui.theme.*

@Composable
fun SearchScreen(
    onMediaSelected: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = 36.dp, end = 32.dp, bottom = 24.dp)
        ) {

            // ─── Search Title ─────────────────────────────────────────
            Text(
                text = "Поиск",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Search Input Field (iOS-style glass pill) ───────────
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = {
                    androidx.compose.material3.Text(
                        text = "Фильмы, сериалы, персоны...",
                        color = TextMutedDark,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        SlooshFocusableCard(
                            onClick = { viewModel.onQueryChanged("") },
                            shape = ContinuousCapsule,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Очистить",
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = GlassSurfaceFocusedDark,
                    unfocusedContainerColor = GlassSurfaceDark,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = GlassBorderUnfocusedDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                shape = ContinuousRoundedRectangle(18.dp),
                modifier = Modifier.fillMaxWidth(0.65f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ─── Content Area ────────────────────────────────────
            when {
                // Recent searches (when query is empty)
                state.query.isEmpty() && state.recentSearches.isNotEmpty() -> {
                    Text(
                        text = "Недавние запросы",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 32.dp)
                    ) {
                        items(state.recentSearches.size, key = { state.recentSearches[it].query }) { idx ->
                            val history = state.recentSearches[idx]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SlooshButton(
                                    text = history.query,
                                    onClick = { viewModel.selectHistoryQuery(history.query) }
                                )
                                // Delete button
                                SlooshFocusableCard(
                                    onClick = { viewModel.deleteHistoryQuery(history.query) },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Удалить",
                                            tint = TextMutedDark,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Empty state (no query, no history)
                state.query.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Начните поиск",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ищите фильмы и сериалы по названию",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedDark
                            )
                        }
                    }
                }

                // Loading
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ищем...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                // No results
                state.results.isEmpty() && state.query.isNotEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Ничего не найдено",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Попробуйте изменить запрос",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedDark
                            )
                        }
                    }
                }

                // Results grid
                else -> {
                    Text(
                        text = "Результаты: ${state.results.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMutedDark,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    TvLazyVerticalGrid(
                        columns = TvGridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.results.size, key = { state.results[it].identifier }) { index ->
                            val item = state.results[index]
                            MediaCard(
                                item = item,
                                onClick = { onMediaSelected(item.identifier) },
                                onFocus = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
