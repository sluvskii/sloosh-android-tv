package com.sloosh.tv.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.home.ContinueWatchingCard
import com.sloosh.tv.ui.home.MediaCard
import com.sloosh.tv.ui.theme.BackgroundDark

enum class FavoriteCategory(val title: String) {
    ALL("Все"),
    MOVIES("Фильмы"),
    SERIES("Сериалы")
}

@Composable
fun ProfileScreen(
    onMediaSelected: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf(FavoriteCategory.ALL) }

    val filteredFavorites = remember(state.favorites, selectedCategory) {
        when (selectedCategory) {
            FavoriteCategory.ALL -> state.favorites
            FavoriteCategory.MOVIES -> state.favorites.filter { it.type == "movie" || it.type == null }
            FavoriteCategory.SERIES -> state.favorites.filter { it.type == "tv" || it.type == "show" || it.type == "series" }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(start = 36.dp, top = 28.dp, end = 36.dp, bottom = 28.dp)
    ) {
        Text(
            text = "Избранное и история",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Continue watching history row
        if (state.progressList.isNotEmpty()) {
            Text(
                text = "Недавно просмотренные",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 32.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(state.progressList, key = { it.mediaId }) { progress ->
                    ContinueWatchingCard(
                        progress = progress,
                        onClick = { onMediaSelected(progress.mediaId) }
                    )
                }
            }
        }

        // Category Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            items(FavoriteCategory.values()) { category ->
                SlooshButton(
                    text = category.title,
                    isPrimary = selectedCategory == category,
                    onClick = { selectedCategory = category }
                )
            }
        }

        if (filteredFavorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Список пуст",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredFavorites, key = { it.mediaId }) { fav ->
                    val dto = MediaDto(
                        originalId = fav.mediaId,
                        title = fav.title,
                        originalTitle = null,
                        year = fav.year,
                        rating = fav.rating,
                        posterUrl = fav.posterUrl,
                        description = null,
                        type = fav.type,
                        genres = null,
                        externalIds = null,
                        name = fav.title,
                        posterPath = fav.posterUrl
                    )
                    MediaCard(
                        item = dto,
                        onClick = { onMediaSelected(fav.mediaId) },
                        onFocus = {}
                    )
                }
            }
        }
    }
}
