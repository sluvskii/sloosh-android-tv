package com.sloosh.tv.ui.continue_watching

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import com.sloosh.tv.ui.components.ContinueGridShimmer
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.components.SlooshFocusableCard
import com.sloosh.tv.ui.theme.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ContinueScreen(
    onMediaSelected: (String) -> Unit,
    viewModel: ContinueViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var selectedItemForAction by remember { mutableStateOf<ContinueWatchingItem?>(null) }
    val firstActionFocusRequester = remember { FocusRequester() }
    val activeItemFocusRequester = remember { FocusRequester() }
    val emptyStateFocusRequester = remember { FocusRequester() }
    val focusBridge = com.sloosh.tv.LocalSideDrawerFocusBridge.current
    val gridState = rememberTvLazyGridState()

    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
    var lastFocusedIndex by rememberSaveable { mutableIntStateOf(0) }

    // ─── Lifecycle Focus Management & Memory Restoration ────────────
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state.items.isNotEmpty()) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (isFirstLaunch) {
                    isFirstLaunch = false
                    try {
                        if (state.items.isNotEmpty()) {
                            activeItemFocusRequester.requestFocus()
                        } else {
                            emptyStateFocusRequester.requestFocus()
                        }
                    } catch (_: Exception) {}
                } else {
                    // Resuming from DetailsScreen or playback
                    if (state.items.isNotEmpty()) {
                        val safeTarget = lastFocusedIndex.coerceIn(0, state.items.lastIndex)
                        coroutineScope.launch {
                            try {
                                gridState.scrollToItem(safeTarget)
                                delay(40)
                                activeItemFocusRequester.requestFocus()
                            } catch (_: Exception) {
                                try { activeItemFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                        }
                    } else {
                        try {
                            emptyStateFocusRequester.requestFocus()
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

    LaunchedEffect(state.items.isNotEmpty()) {
        if (state.items.isNotEmpty() && !focusBridge.isDrawerOpen && isFirstLaunch) {
            try {
                activeItemFocusRequester.requestFocus()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    DisposableEffect(focusBridge, state.items.isNotEmpty(), lastFocusedIndex) {
        focusBridge.contentFocusCallback = {
            if (state.items.isNotEmpty()) {
                val safeTarget = lastFocusedIndex.coerceIn(0, state.items.lastIndex)
                try {
                    activeItemFocusRequester.requestFocus()
                } catch (e: Exception) {
                    coroutineScope.launch {
                        try {
                            gridState.scrollToItem(safeTarget)
                            delay(32)
                            activeItemFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                    }
                }
            } else {
                try {
                    emptyStateFocusRequester.requestFocus()
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

    // Close action dialog on Back button
    BackHandler(enabled = selectedItemForAction != null) {
        selectedItemForAction = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(start = 16.dp, top = 32.dp, end = 24.dp, bottom = 32.dp)
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
                ContinueGridShimmer()
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

                        Spacer(modifier = Modifier.height(24.dp))

                        SlooshButton(
                            text = "В каталог",
                            icon =
                                {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                try {
                                    focusBridge.drawerNavFocusRequesters[com.sloosh.tv.ui.components.NavSection.HOME]?.requestFocus()
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                            modifier = Modifier
                                .focusRequester(emptyStateFocusRequester)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                        keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                                    ) {
                                        try {
                                            focusBridge.drawerNavFocusRequesters[com.sloosh.tv.ui.components.NavSection.CONTINUE]?.requestFocus()
                                            true
                                        } catch (e: Exception) {
                                            false
                                        }
                                    } else false
                                }
                        )
                    }
                }
            } else {
                // Grid of Continue Watching Cards (Wide 16:9 cards)
                TvLazyVerticalGrid(
                    state = gridState,
                    columns = TvGridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 40.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items.size, key = { index -> "${state.items[index].id}_$index" }) { index ->
                        val item = state.items[index]
                        val isFirstColumn = index % 3 == 0
                        val targetIndex = lastFocusedIndex.coerceIn(0, (state.items.size - 1).coerceAtLeast(0))
                        val isTarget = (index == targetIndex)

                        val cardModifier = Modifier
                            .then(if (isTarget) Modifier.focusRequester(activeItemFocusRequester) else Modifier)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    lastFocusedIndex = index
                                }
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                    keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT &&
                                    isFirstColumn
                                ) {
                                    try {
                                        focusBridge.drawerNavFocusRequesters[com.sloosh.tv.ui.components.NavSection.CONTINUE]?.requestFocus()
                                        true
                                    } catch (e: Exception) {
                                        false
                                    }
                                } else false
                            }

                        ContinueWatchingCard(
                            item = item,
                            onClick = { onMediaSelected(item.mediaId) },
                            onLongClick = { selectedItemForAction = item },
                            onMenuClick = { selectedItemForAction = item },
                            modifier = cardModifier
                        )
                    }
                }
            }
        }

        // Action / Context Dialog for an Item (With guaranteed D-pad focus capture)
        AnimatedVisibility(
            visible = selectedItemForAction != null,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            val item = selectedItemForAction
            if (item != null) {
                LaunchedEffect(Unit) {
                    delay(60)
                    try { firstActionFocusRequester.requestFocus() } catch (_: Exception) {}
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(440.dp)
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

                        val metaText = if (item.isEpisode && item.season != null && item.episode != null) {
                            "Сезон ${item.season} • Серия ${item.episode}  •  ${item.remainingLabel}"
                        } else {
                            "${item.timeLabel}  •  ${item.remainingLabel}"
                        }

                        Text(
                            text = metaText,
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(firstActionFocusRequester)
                            )

                            SlooshButton(
                                text = "Открыть описание",
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

private val ContinueCardShape = ContinuousRoundedRectangle(18.dp)

@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.15f to Color.Black.copy(alpha = 0.10f),
                0.50f to Color.Black.copy(alpha = 0.35f),
                1.0f to Color.Black.copy(alpha = 0.92f)
            )
        )
    }

    SlooshFocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_MENU) {
                        onMenuClick()
                        true
                    } else false
                } else false
            },
        shape = ContinueCardShape
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ContinueCardShape)
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
                    .background(gradientBrush)
            )

            // "Следующая серия" badge top-left if applicable
            if (item.isNextEpisode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(ContinuousCapsule)
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Следующая серия",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color.Black
                    )
                }
            }

            // Options button top-right (visible on focus for remote users)
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(ContinuousCapsule)
                        .background(Color.Black.copy(alpha = 0.60f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Опции",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Play indicator badge in center on focus
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
                if (!item.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.logoUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .height(36.dp)
                            .widthIn(max = 180.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
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
                }

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

                if (!item.isNextEpisode) {
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
}
