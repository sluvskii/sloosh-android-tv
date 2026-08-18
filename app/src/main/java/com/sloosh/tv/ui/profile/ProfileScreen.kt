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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer

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

    val categories = FavoriteCategory.values()
    val categoryFocusRequesters = remember { Array(categories.size) { FocusRequester() } }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val categoryTitles = remember(categoryCounts) {
        categories.map { cat ->
            val count = categoryCounts[cat] ?: 0
            if (count > 0) "${cat.title} $count" else cat.title
        }
    }

    val tabWidths = remember(categoryTitles, density) {
        categoryTitles.map { title ->
            val textLayoutResult = textMeasurer.measure(
                text = title,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
            )
            val measuredWidthDp = with(density) { textLayoutResult.size.width.toDp() }
            (measuredWidthDp + 26.dp).coerceAtLeast(64.dp)
        }
    }

    val selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
    val targetWidth = tabWidths.getOrElse(selectedIndex) { 80.dp }

    val targetOffset = remember(selectedIndex, tabWidths) {
        var acc = 0.dp
        for (i in 0 until selectedIndex) {
            acc += tabWidths.getOrElse(i) { 80.dp }
        }
        acc
    }

    val animatedPillOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 400f),
        label = "favPillOffset"
    )

    val animatedPillWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 400f),
        label = "favPillWidth"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp, top = 32.dp, end = 16.dp, bottom = 24.dp)
        ) {

            // ─── Header ──────────────────────────────────────────────
            Text(
                text = "Избранное",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ─── Continuous Segmented Capsule ────────────────────────
            Box(
                modifier = Modifier
                    .clip(ContinuousCapsule)
                    .background(Color(0xFF141416).copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = ContinuousCapsule
                    )
                    .padding(3.5.dp)
            ) {
                // Sliding White Capsule Pill
                if (targetWidth > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset(x = animatedPillOffset)
                            .width(animatedPillWidth)
                            .height(34.dp)
                            .clip(ContinuousCapsule)
                            .background(Color.White)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    categories.forEachIndexed { index, cat ->
                        val isSelected = selectedCategory == cat
                        val thisTabWidth = tabWidths.getOrElse(index) { 80.dp }
                        val titleText = categoryTitles.getOrElse(index) { cat.title }

                        Box(
                            modifier = Modifier
                                .width(thisTabWidth)
                                .height(34.dp)
                                .clip(ContinuousCapsule)
                                .focusRequester(categoryFocusRequesters[index])
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && selectedCategory != cat) {
                                        selectedCategory = cat
                                    }
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedCategory = cat
                                }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                if (index > 0) {
                                                    try {
                                                        categoryFocusRequesters[index - 1].requestFocus()
                                                        true
                                                    } catch (e: Exception) {
                                                        false
                                                    }
                                                } else false
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                if (index < categories.size - 1) {
                                                    try {
                                                        categoryFocusRequesters[index + 1].requestFocus()
                                                        true
                                                    } catch (e: Exception) {
                                                        false
                                                    }
                                                } else false
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                val context = androidx.compose.ui.platform.LocalContext.current
                val appSettings = remember { com.sloosh.tv.data.repository.AppSettings(context) }
                val gridColumns = appSettings.gridColumns
                val isCompact = gridColumns >= 6
                val gridSpacing = if (isCompact) 12.dp else 16.dp

                TvLazyVerticalGrid(
                    columns = TvGridCells.Fixed(gridColumns),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    contentPadding = PaddingValues(bottom = 60.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFavorites.size, key = { filteredFavorites[it].identifier }) { index ->
                        val item = filteredFavorites[index]
                        MediaCard(
                            item = item,
                            onClick = { onMediaSelected(item.identifier) },
                            compact = isCompact,
                            onFocus = {}
                        )
                    }
                }
            }
        }
    }
}
