package com.sloosh.tv.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.sloosh.tv.data.api.MediaDto
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.sloosh.tv.ui.components.*
import com.sloosh.tv.ui.theme.*

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import kotlinx.coroutines.launch
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onMediaSelected: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val gridState = rememberTvLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val categories = HomeCategory.values()
    val focusBridge = com.sloosh.tv.LocalSideDrawerFocusBridge.current
    val activeCardFocusRequester = remember { FocusRequester() }
    val categoryFocusRequesters = remember { Array(categories.size) { FocusRequester() } }
    var lastFocusedArea by rememberSaveable { mutableStateOf("tab") }
    var lastFocusedCardIndex by rememberSaveable { mutableIntStateOf(0) }

    val pageCategory = state.selectedCategory
    val categoryItems = state.categoryItems[pageCategory] ?: emptyList()

    // ─── Initial D-pad Focus on Startup ─────────────────────────────
    // 1. Initially focus category tab 0 so remote navigation is active immediately
    LaunchedEffect(Unit) {
        try {
            categoryFocusRequesters[0].requestFocus()
        } catch (e: Exception) {
            // ignore
        }
    }

    // 2. Once catalog items load, smoothly focus the active card in the grid if user moved to card area
    LaunchedEffect(categoryItems.isNotEmpty()) {
        if (categoryItems.isNotEmpty() && !focusBridge.isDrawerOpen && lastFocusedArea == "card") {
            try {
                activeCardFocusRequester.requestFocus()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // 3. Reset card focus index when category tab changes
    LaunchedEffect(state.selectedCategory) {
        lastFocusedCardIndex = 0
    }

    // 4. Register side drawer exit focus callback with exact card index restoration
    DisposableEffect(focusBridge, lastFocusedArea, state.selectedCategory, categoryItems.isNotEmpty(), lastFocusedCardIndex) {
        focusBridge.contentFocusCallback = {
            var focused = false
            if (lastFocusedArea == "card" && categoryItems.isNotEmpty()) {
                val safeTarget = lastFocusedCardIndex.coerceIn(0, categoryItems.lastIndex)
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
                    categoryFocusRequesters[state.selectedCategory.ordinal].requestFocus()
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

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        // Fullscreen Native TV Poster Grid (Offset cleanly alongside side drawer dock)
        val context = androidx.compose.ui.platform.LocalContext.current
        val appSettings = remember { com.sloosh.tv.data.repository.AppSettings(context) }
        val gridColumns = appSettings.gridColumns
        val isCompact = gridColumns >= 6
        val gridSpacing = if (isCompact) 12.dp else 16.dp

        val isCurrentLoading = state.isLoading && categoryItems.isEmpty()

        if (isCurrentLoading) {
            com.sloosh.tv.ui.components.PosterGridShimmer(
                gridColumns = gridColumns,
                isCompact = isCompact,
                modifier = Modifier.padding(start = 12.dp, top = 60.dp, end = 20.dp)
            )
        } else {
            androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid(
                state = gridState,
                columns = androidx.tv.foundation.lazy.grid.TvGridCells.Fixed(gridColumns),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                contentPadding = PaddingValues(start = 12.dp, top = 75.dp, end = 20.dp, bottom = 40.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Poster Items
                items(categoryItems.size, key = { index -> "${pageCategory.name}_${categoryItems[index].identifier}_$index" }) { index ->
                    val item = categoryItems[index]
                    if (index >= categoryItems.size - 4 && !state.isLoadingMore && state.hasMorePages) {
                        LaunchedEffect(index) {
                            viewModel.loadData(reset = false)
                        }
                    }

                    val isFirstColumn = index % gridColumns == 0
                    val isTopRow = index < gridColumns
                    val targetIndex = lastFocusedCardIndex.coerceIn(0, (categoryItems.size - 1).coerceAtLeast(0))
                    val isTargetCard = (index == targetIndex)

                    val cardModifier = Modifier
                        .then(if (isTargetCard) Modifier.focusRequester(activeCardFocusRequester) else Modifier)
                        .onFocusChanged {
                            if (it.isFocused) {
                                lastFocusedArea = "card"
                                lastFocusedCardIndex = index
                            }
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                        if (isTopRow) {
                                            try {
                                                categoryFocusRequesters[state.selectedCategory.ordinal].requestFocus()
                                                true
                                            } catch (e: Exception) {
                                                false
                                            }
                                        } else false
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        if (isFirstColumn) {
                                            try {
                                                focusBridge.drawerNavFocusRequesters[com.sloosh.tv.ui.components.NavSection.HOME]?.requestFocus()
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
                        onClick = { onMediaSelected(item.identifier) },
                        compact = isCompact,
                        modifier = cardModifier,
                        onFocus = {
                            lastFocusedArea = "card"
                            lastFocusedCardIndex = index
                        }
                    )
                }

                // Loading footer when fetching more items
                if (state.isLoadingMore) {
                    item(span = { androidx.tv.foundation.lazy.grid.TvGridItemSpan(gridColumns) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }

        // Reusable top scrim gradient
        val topScrimGradient = remember {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF09090A).copy(alpha = 0.88f),
                    0.40f to Color(0xFF09090A).copy(alpha = 0.58f),
                    0.72f to Color(0xFF09090A).copy(alpha = 0.20f),
                    1.00f to Color.Transparent
                )
            )
        }

        // Top Gradient Scrim for Floating Tab Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopCenter)
                .background(topScrimGradient)
        )

        // Floating Sticky Segmented Category Capsule Bar with Adaptive Text-Width Sliding Pill
        val density = LocalDensity.current
        val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
        val tabTextStyle = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        // Measure natural widths for each category tab + 36.dp (18.dp horizontal padding on each side)
        val tabWidths = remember(density) {
            categories.map { cat ->
                with(density) {
                    val measuredTextWidth = textMeasurer.measure(cat.title, tabTextStyle).size.width.toDp()
                    measuredTextWidth + 36.dp
                }
            }
        }

        // Calculate exact cumulative x-offsets for each tab
        val tabOffsets = remember(tabWidths) {
            val offsets = mutableListOf<androidx.compose.ui.unit.Dp>()
            var currentX = 0.dp
            tabWidths.forEach { w ->
                offsets.add(currentX)
                currentX += w
            }
            offsets
        }

        val targetOffset = tabOffsets.getOrElse(state.selectedCategory.ordinal) { 0.dp }
        val targetWidth = tabWidths.getOrElse(state.selectedCategory.ordinal) { 0.dp }

        // Physical spring animation with mass, velocity and natural settle for BOTH offset and width
        val animatedPillOffset by androidx.compose.animation.core.animateDpAsState(
            targetValue = targetOffset,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.74f,
                stiffness = 380f
            ),
            label = "tabPillOffset"
        )

        val animatedPillWidth by androidx.compose.animation.core.animateDpAsState(
            targetValue = targetWidth,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.74f,
                stiffness = 380f
            ),
            label = "tabPillWidth"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(start = 12.dp, top = 15.dp, end = 20.dp, bottom = 20.dp),
            contentAlignment = Alignment.Center
        ) {
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
                // 1. Sliding White Capsule Pill (Adapts width & offset with spring physics)
                if (targetWidth > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset(x = animatedPillOffset)
                            .width(animatedPillWidth)
                            .height(36.dp)
                            .clip(ContinuousCapsule)
                            .background(Color.White)
                    )
                }

                // 2. Interactive Category Tabs (Each with its exact proportional width)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEachIndexed { index, cat ->
                        val isSelected = state.selectedCategory == cat
                        val thisTabWidth = tabWidths.getOrElse(index) { 90.dp }

                        Box(
                            modifier = Modifier
                                .width(thisTabWidth)
                                .height(36.dp)
                                .clip(ContinuousCapsule)
                                .focusable()
                                .focusRequester(categoryFocusRequesters[index])
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        lastFocusedArea = "tab"
                                        viewModel.selectCategory(cat)
                                    }
                                }
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    viewModel.selectCategory(cat)
                                    coroutineScope.launch { gridState.scrollToItem(0) }
                                }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                             android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                 if (categoryItems.isNotEmpty()) {
                                                     val safeTarget = lastFocusedCardIndex.coerceIn(0, categoryItems.lastIndex)
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
                                                        focusBridge.drawerNavFocusRequesters[com.sloosh.tv.ui.components.NavSection.HOME]?.requestFocus()
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
                                }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}


private val StandardPosterShape = ContinuousRoundedRectangle(16.dp)
private val CompactPosterShape = ContinuousRoundedRectangle(14.dp)
private val StandardBadgeShape = ContinuousRoundedRectangle(7.dp)
private val CompactBadgeShape = ContinuousRoundedRectangle(6.dp)

@Composable
fun MediaCard(
    item: MediaDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onFocus: (() -> Unit)? = null
) {
    val posterShape = if (compact) CompactPosterShape else StandardPosterShape
    val badgeShape = if (compact) CompactBadgeShape else StandardBadgeShape

    Column(modifier = Modifier.fillMaxWidth()) {
        // ─── Poster (True 2:3 aspect ratio, no top/bottom cropping) ─────
        SlooshFocusableCard(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = posterShape,
            focusedScale = 1.08f
        ) { cardFocused ->
            if (cardFocused && onFocus != null) {
                LaunchedEffect(Unit) { onFocus() }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.getDisplayPosterUrl(),
                    contentDescription = item.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Adaptive rating badge top-left (proportional to compact mode)
                if (item.rating != null && item.rating > 0) {
                    val badgePaddingHorizontal = if (compact) 5.dp else 6.5.dp
                    val badgePaddingVertical = if (compact) 2.dp else 2.5.dp
                    val badgeFontSize = if (compact) 12.sp else 13.5.sp
                    val badgeMargin = if (compact) 6.dp else 8.dp

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(badgeMargin)
                            .clip(badgeShape)
                            .background(ratingColor(item.rating))
                            .padding(horizontal = badgePaddingHorizontal, vertical = badgePaddingVertical),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", item.rating),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = badgeFontSize,
                                letterSpacing = (-0.2).sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeight = badgeFontSize
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ─── Title + Meta BELOW ───────────────────────────────────────────────
        val topSpacer = if (compact) 5.5.dp else 7.dp
        val titleSize = if (compact) 13.5.sp else 15.sp
        val titleLineHeight = if (compact) 17.sp else 19.sp
        val metaSize = if (compact) 11.5.sp else 12.5.sp

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(topSpacer))
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = titleSize,
                    lineHeight = titleLineHeight
                ),
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            val genreText: String? = item.genres?.firstOrNull()?.name
            val metaText: String = listOfNotNull(item.yearString.ifEmpty { null }, genreText).joinToString(" • ")
            if (metaText.isNotEmpty()) {
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = metaSize,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color.White.copy(alpha = 0.50f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}
