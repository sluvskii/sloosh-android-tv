package com.sloosh.tv.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
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
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.home.MediaCard
import com.sloosh.tv.ui.theme.*
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

// iOS ProfileView has 4 categories: Все / Фильмы / Сериалы / Мульты
enum class FavoriteCategory(val title: String) {
    ALL("Все"),
    MOVIES("Фильмы"),
    SERIES("Сериалы"),
    CARTOONS("Мульты")
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
        val raw = when (selectedCategory) {
            FavoriteCategory.ALL -> state.favorites
            FavoriteCategory.MOVIES -> state.favorites.filter {
                it.type == "movie" || it.type == null
            }
            FavoriteCategory.SERIES -> state.favorites.filter {
                it.type == "tv" || it.type == "show" || it.type == "series"
            }
            FavoriteCategory.CARTOONS -> state.favorites.filter {
                it.type == "cartoon" || it.type == "animation"
            }
        }
        raw.map { fav ->
            MediaDto(
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
                posterPath = null
            )
        }
    }

    val categoryCounts = remember(state.favorites) {
        FavoriteCategory.values().associateWith { cat ->
            when (cat) {
                FavoriteCategory.ALL -> state.favorites.size
                FavoriteCategory.MOVIES -> state.favorites.count { it.type == "movie" || it.type == null }
                FavoriteCategory.SERIES -> state.favorites.count { it.type == "tv" || it.type == "show" || it.type == "series" }
                FavoriteCategory.CARTOONS -> state.favorites.count { it.type == "cartoon" || it.type == "animation" }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = 32.dp, end = 32.dp, bottom = 24.dp)
        ) {

            // ─── Header ──────────────────────────────────────────────
            Text(
                text = "Избранное",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Category Filter Tabs with counts ─────────────────────
            val categories = FavoriteCategory.values()
            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                items(categories.size, key = { categories[it].name }) { idx ->
                    val category = categories[idx]
                    val count = categoryCounts[category] ?: 0
                    SlooshButton(
                        text = if (count > 0) "${category.title} $count" else category.title,
                        isPrimary = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            // ─── Content ──────────────────────────────────────────────
            if (filteredFavorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(ContinuousRoundedRectangle(20.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Список пуст",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Добавляйте фильмы в избранное\nи они появятся здесь",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMutedDark
                        )
                    }
                }
            } else {
                TvLazyVerticalGrid(
                    columns = TvGridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 60.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFavorites.size, key = { filteredFavorites[it].identifier }) { index ->
                        val item = filteredFavorites[index]
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
