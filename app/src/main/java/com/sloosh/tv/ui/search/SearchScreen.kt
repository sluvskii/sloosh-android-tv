package com.sloosh.tv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent

@Composable
fun SearchScreen(
    onMediaSelected: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val searchInputFocusRequester = remember { FocusRequester() }
    val activeRecentFocusRequester = remember { FocusRequester() }
    val activeResultFocusRequester = remember { FocusRequester() }
    val focusBridge = com.sloosh.tv.LocalSideDrawerFocusBridge.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val appSettings = remember { com.sloosh.tv.data.repository.AppSettings(context) }
    val gridColumns = appSettings.gridColumns
    val isCompact = gridColumns >= 6
    val horizontalGridSpacing = if (isCompact) 4.dp else 6.dp
    val verticalGridSpacing = if (isCompact) 2.dp else 4.dp
    val resultsGridState = rememberTvLazyGridState()

    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
    var lastFocusedArea by rememberSaveable { mutableStateOf("input") }
    var lastFocusedResultIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastFocusedRecentIndex by rememberSaveable { mutableIntStateOf(0) }

    // ─── Lifecycle Focus Management & Memory Restoration ────────────
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, lastFocusedArea, state.results.isNotEmpty(), state.recentSearches.isNotEmpty()) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (isFirstLaunch) {
                    isFirstLaunch = false
                    try {
                        searchInputFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                } else {
                    // Resuming from child screen (DetailsScreen, etc.)
                    if (lastFocusedArea == "result" && state.results.isNotEmpty()) {
                        val safeTarget = lastFocusedResultIndex.coerceIn(0, state.results.lastIndex)
                        coroutineScope.launch {
                            try {
                                resultsGridState.scrollToItem(safeTarget)
                                delay(40)
                                activeResultFocusRequester.requestFocus()
                            } catch (_: Exception) {
                                try { activeResultFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                        }
                    } else if (lastFocusedArea == "recent" && state.recentSearches.isNotEmpty()) {
                        try {
                            activeRecentFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                    } else {
                        try {
                            searchInputFocusRequester.requestFocus()
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

    // Reset result focus index on query change
    LaunchedEffect(state.query) {
        lastFocusedResultIndex = 0
    }

    DisposableEffect(focusBridge, lastFocusedArea, state.results.isNotEmpty(), state.recentSearches.isNotEmpty(), lastFocusedResultIndex, lastFocusedRecentIndex) {
        focusBridge.contentFocusCallback = {
            var focused = false
            if (lastFocusedArea == "result" && state.results.isNotEmpty()) {
                val safeTarget = lastFocusedResultIndex.coerceIn(0, state.results.lastIndex)
                try {
                    activeResultFocusRequester.requestFocus()
                    focused = true
                } catch (e: Exception) {
                    coroutineScope.launch {
                        try {
                            resultsGridState.scrollToItem(safeTarget)
                            delay(32)
                            activeResultFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                    }
                    focused = true
                }
            } else if (lastFocusedArea == "recent" && state.recentSearches.isNotEmpty()) {
                try {
                    activeRecentFocusRequester.requestFocus()
                    focused = true
                } catch (e: Exception) {
                    // fall back
                }
            }
            if (!focused) {
                try {
                    searchInputFocusRequester.requestFocus()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        onDispose {
            if (focusBridge.contentFocusCallback != null) {
                focusBridge.contentFocusCallback = null
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
                .padding(start = 6.dp, top = 36.dp, end = 10.dp, bottom = 24.dp)
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
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .focusRequester(searchInputFocusRequester)
                    .onFocusChanged { if (it.isFocused) lastFocusedArea = "input" }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    try {
                                        if (state.query.isEmpty() && state.recentSearches.isNotEmpty()) {
                                            activeRecentFocusRequester.requestFocus()
                                            true
                                        } else if (state.results.isNotEmpty()) {
                                            val safeTarget = lastFocusedResultIndex.coerceIn(0, state.results.lastIndex)
                                            try {
                                                activeResultFocusRequester.requestFocus()
                                                true
                                            } catch (e: Exception) {
                                                coroutineScope.launch {
                                                    try {
                                                        resultsGridState.scrollToItem(safeTarget)
                                                        delay(32)
                                                        activeResultFocusRequester.requestFocus()
                                                    } catch (_: Exception) {}
                                                }
                                                true
                                            }
                                        } else false
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    try {
                                        focusBridge.drawerNavFocusRequesters[com.sloosh.tv.ui.components.NavSection.SEARCH]?.requestFocus()
                                        true
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else false
                    }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ─── Animated Content Area ───────────────────────────
            val searchStateKey = when {
                state.query.isEmpty() && state.recentSearches.isNotEmpty() -> 0
                state.query.isEmpty() -> 1
                state.isLoading -> 2
                state.results.isEmpty() -> 3
                else -> 4
            }

            AnimatedContent(
                targetState = searchStateKey,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
                },
                label = "searchContentTransition",
                modifier = Modifier.fillMaxSize()
            ) { targetKey ->
                when (targetKey) {
                    0 -> {
                        // Recent searches
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 14.dp)
                            ) {
                                Text(
                                    text = "Недавние запросы",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                SlooshFocusableCard(
                                    onClick = { viewModel.clearAllHistory() },
                                    shape = ContinuousCapsule,
                                    modifier = Modifier.wrapContentSize()
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .clip(ContinuousCapsule)
                                            .background(if (isFocused) Color.White else Color.White.copy(alpha = 0.10f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Очистить всё",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = if (isFocused) Color.Black else Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                            TvLazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(end = 32.dp)
                            ) {
                                items(state.recentSearches.size, key = { "${state.recentSearches[it].query}_$it" }) { idx ->
                                    val history = state.recentSearches[idx]
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val targetRecentIndex = lastFocusedRecentIndex.coerceIn(0, (state.recentSearches.size - 1).coerceAtLeast(0))
                                        val isTargetRecent = (idx == targetRecentIndex)
                                        val btnModifier = (if (isTargetRecent) Modifier.focusRequester(activeRecentFocusRequester) else Modifier)
                                            .onFocusChanged {
                                                if (it.isFocused) {
                                                    lastFocusedArea = "recent"
                                                    lastFocusedRecentIndex = idx
                                                }
                                            }
                                        SlooshButton(
                                            text = history.query,
                                            onClick = { viewModel.selectHistoryQuery(history.query) },
                                            modifier = btnModifier.onPreviewKeyEvent { keyEvent ->
                                                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                            try {
                                                                searchInputFocusRequester.requestFocus()
                                                                true
                                                            } catch (e: Exception) {
                                                                false
                                                            }
                                                        }
                                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                            if (idx == 0) {
                                                                try {
                                                                    focusBridge.drawerNavFocusRequesters[com.sloosh.tv.ui.components.NavSection.SEARCH]?.requestFocus()
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
                                        )
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
                    }
                    1 -> {
                        // Empty initial state
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                    2 -> {
                        // Loading shimmer
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Ищем...",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMutedDark,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                            com.sloosh.tv.ui.components.PosterGridShimmer(
                                gridColumns = gridColumns,
                                isCompact = isCompact,
                                itemCount = 10
                            )
                        }
                    }
                    3 -> {
                        // No results found
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                    4 -> {
                        // Results grid
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Результаты: ${state.results.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMutedDark,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                            TvLazyVerticalGrid(
                                state = resultsGridState,
                                columns = TvGridCells.Fixed(gridColumns),
                                horizontalArrangement = Arrangement.spacedBy(horizontalGridSpacing),
                                verticalArrangement = Arrangement.spacedBy(verticalGridSpacing),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.results.size, key = { index -> "${state.results[index].identifier}_$index" }) { index ->
                                    val item = state.results[index]
                                    val isFirstColumn = index % gridColumns == 0
                                    val isTopRow = index < gridColumns
                                    val targetResultIndex = lastFocusedResultIndex.coerceIn(0, (state.results.size - 1).coerceAtLeast(0))
                                    val isTargetResult = (index == targetResultIndex)

                                    val cardModifier = Modifier
                                        .then(if (isTargetResult) Modifier.focusRequester(activeResultFocusRequester) else Modifier)
                                        .onFocusChanged {
                                            if (it.isFocused) {
                                                lastFocusedArea = "result"
                                                lastFocusedResultIndex = index
                                            }
                                        }
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                                when (keyEvent.nativeKeyEvent.keyCode) {
                                                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                        if (isTopRow) {
                                                            try {
                                                                searchInputFocusRequester.requestFocus()
                                                                true
                                                            } catch (e: Exception) {
                                                                false
                                                            }
                                                        } else false
                                                    }
                                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                        if (isFirstColumn) {
                                                            try {
                                                                focusBridge.drawerNavFocusRequesters[com.sloosh.tv.ui.components.NavSection.SEARCH]?.requestFocus()
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
                                            lastFocusedArea = "result"
                                            lastFocusedResultIndex = index
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
