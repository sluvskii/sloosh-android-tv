package com.sloosh.tv.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// iOS ProfileView has 5 categories: Все / Фильмы / Сериалы / Мульты / Аниме
enum class FavoriteCategory(val title: String) {
    ALL("Все"),
    MOVIES("Фильмы"),
    SERIES("Сериалы"),
    CARTOONS("Мульты"),
    ANIME("Аниме")
}

@Composable
fun ProfileScreen(
    onMediaSelected: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by rememberSaveable { mutableStateOf(FavoriteCategory.ALL) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val appSettings = remember { com.sloosh.tv.data.repository.AppSettings(context) }
    val gridColumns = appSettings.gridColumns
    val isCompact = gridColumns >= 6
    val horizontalGridSpacing = if (isCompact) 4.dp else 6.dp
    val verticalGridSpacing = if (isCompact) 2.dp else 4.dp
    val activeCardFocusRequester = remember { FocusRequester() }
    val focusBridge = com.sloosh.tv.LocalSideDrawerFocusBridge.current
    val categories = FavoriteCategory.values()
    val categoryFocusRequesters = remember { Array(categories.size) { FocusRequester() } }
    val gridState = rememberTvLazyGridState()

    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
    var lastFocusedArea by rememberSaveable { mutableStateOf("tab") }
    val categoryCardIndices = rememberSaveable { mutableStateMapOf<String, Int>() }

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
            FavoriteCategory.ANIME -> state.favorites.filter {
                it.type == "anime"
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

    // ─── Lifecycle Focus Management & Memory Restoration ────────────
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, selectedCategory, filteredFavorites.isNotEmpty()) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (isFirstLaunch) {
                    isFirstLaunch = false
                    try {
                        categoryFocusRequesters[0].requestFocus()
                    } catch (_: Exception) {}
                } else {
                    // Resuming from DetailsScreen or back navigation
                    if (lastFocusedArea == "card" && filteredFavorites.isNotEmpty()) {
                        val savedIndex = categoryCardIndices[selectedCategory.name] ?: 0
                        val safeTarget = savedIndex.coerceIn(0, filteredFavorites.lastIndex)
                        coroutineScope.launch {
                            try {
                                gridState.scrollToItem(safeTarget)
                                delay(40)
                                activeCardFocusRequester.requestFocus()
                            } catch (_: Exception) {
                                try { activeCardFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                        }
                    } else {
                        try {
                            categoryFocusRequesters[selectedCategory.ordinal].requestFocus()
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Scroll to category saved position when category tab changes
    LaunchedEffect(selectedCategory) {
        val savedIndex = (categoryCardIndices[selectedCategory.name] ?: 0).coerceIn(0, (filteredFavorites.size - 1).coerceAtLeast(0))
        try {
            gridState.scrollToItem(savedIndex)
        } catch (_: Exception) {}
    }

    DisposableEffect(focusBridge, lastFocusedArea, selectedCategory, filteredFavorites.isNotEmpty()) {
        focusBridge.contentFocusCallback = {
            var focused = false
            if (lastFocusedArea == "card" && filteredFavorites.isNotEmpty()) {
                val savedIndex = categoryCardIndices[selectedCategory.name] ?: 0
                val safeTarget = savedIndex.coerceIn(0, filteredFavorites.lastIndex)
                try {
                    activeCardFocusRequester.requestFocus()
                    focused = true
                } catch (e: Exception) {
                    coroutineScope.launch {
                        try {
                            gridState.scrollToItem(safeTarget)
                            delay(32)
                            activeCardFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                    }
                    focused = true
                }
            }
            if (!focused) {
                try {
                    categoryFocusRequesters[selectedCategory.ordinal].requestFocus()
                } catch (e: Exception) {
                    try {
                        categoryFocusRequesters[0].requestFocus()
                    } catch (e2: Exception) {}
                }
            }
        }
        onDispose {
            if (focusBridge.contentFocusCallback != null) {
                focusBridge.contentFocusCallback = null
            }
        }
    }

    val categoryCounts = remember(state.favorites) {
        FavoriteCategory.values().associateWith { cat ->
            when (cat) {
                FavoriteCategory.ALL -> state.favorites.size
                FavoriteCategory.MOVIES -> state.favorites.count { it.type == "movie" || it.type == null }
                FavoriteCategory.SERIES -> state.favorites.count { it.type == "tv" || it.type == "show" || it.type == "series" }
                FavoriteCategory.CARTOONS -> state.favorites.count { it.type == "cartoon" || it.type == "animation" }
                FavoriteCategory.ANIME -> state.favorites.count { it.type == "anime" }
            }
        }
    }

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
                .padding(start = 6.dp, top = 32.dp, end = 10.dp, bottom = 24.dp)
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
                                .focusable()
                                .focusRequester(categoryFocusRequesters[index])
                                .onFocusChanged { focusState: FocusState ->
                                    if (focusState.isFocused) {
                                        lastFocusedArea = "tab"
                                        if (selectedCategory != cat) {
                                            selectedCategory = cat
                                        }
                                    }
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedCategory = cat
                                }
                                .onPreviewKeyEvent { keyEvent: KeyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                if (filteredFavorites.isNotEmpty()) {
                                                    val safeTarget = (categoryCardIndices[cat.name] ?: 0).coerceIn(0, filteredFavorites.lastIndex)
                                                    try {
                                                        activeCardFocusRequester.requestFocus()
                                                        true
                                                    } catch (e: Exception) {
                                                        coroutineScope.launch {
                                                            try {
                                                                gridState.scrollToItem(safeTarget)
                                                                delay(32)
                                                                activeCardFocusRequester.requestFocus()
                                                            } catch (_: Exception) {}
                                                        }
                                                        true
                                                    }
                                                } else false
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                if (index > 0) {
                                                    try {
                                                        categoryFocusRequesters[index - 1].requestFocus()
                                                        true
                                                    } catch (e: Exception) {
                                                        false
                                                    }
                                                } else {
                                                    try {
                                                        focusBridge.drawerNavFocusRequesters[com.sloosh.tv.ui.components.NavSection.FAVORITES]?.requestFocus()
                                                        true
                                                    } catch (e: Exception) {
                                                        false
                                                    }
                                                }
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
                TvLazyVerticalGrid(
                    state = gridState,
                    columns = TvGridCells.Fixed(gridColumns),
                    horizontalArrangement = Arrangement.spacedBy(horizontalGridSpacing),
                    verticalArrangement = Arrangement.spacedBy(verticalGridSpacing),
                    contentPadding = PaddingValues(bottom = 60.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFavorites.size, key = { index -> "${filteredFavorites[index].identifier}_$index" }) { index ->
                        val item = filteredFavorites[index]
                        val isFirstColumn = index % gridColumns == 0
                        val isTopRow = index < gridColumns
                        val savedIndex = categoryCardIndices[selectedCategory.name] ?: 0
                        val targetIndex = savedIndex.coerceIn(0, (filteredFavorites.size - 1).coerceAtLeast(0))
                        val isTargetCard = (index == targetIndex)

                        val cardModifier = Modifier
                            .then(if (isTargetCard) Modifier.focusRequester(activeCardFocusRequester) else Modifier)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    lastFocusedArea = "card"
                                    categoryCardIndices[selectedCategory.name] = index
                                }
                            }
                            .onPreviewKeyEvent { keyEvent: KeyEvent ->
                                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                            if (isTopRow) {
                                                try {
                                                    categoryFocusRequesters[selectedCategory.ordinal].requestFocus()
                                                    true
                                                } catch (e: Exception) {
                                                    false
                                                }
                                            } else false
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            if (isFirstColumn) {
                                                try {
                                                    focusBridge.drawerNavFocusRequesters[com.sloosh.tv.ui.components.NavSection.FAVORITES]?.requestFocus()
                                                    true
                                                } catch (e: Exception) {
                                                    false
                                                }
                                            } else false
                                        }
                                        else -> false
                                    }
                                } else false
                            }

                        MediaCard(
                            item = item,
                            onClick = {
                                com.sloosh.tv.data.repository.MoviesRepository.instance.setPreviewDetails(item)
                                onMediaSelected(item.identifier)
                            },
                            compact = isCompact,
                            modifier = cardModifier,
                            onFocus = {
                                lastFocusedArea = "card"
                                categoryCardIndices[selectedCategory.name] = index
                            }
                        )
                    }
                }
            }
        }
    }
}
