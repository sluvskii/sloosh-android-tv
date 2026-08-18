package com.sloosh.tv.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun HomeScreen(
    onMediaSelected: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val gridState = rememberTvLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val firstCardFocusRequester = remember { FocusRequester() }
    val categoryFocusRequesters = remember { Array(HomeCategory.values().size) { FocusRequester() } }

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        // Fullscreen Edge-to-Edge Native TV Poster Grid (Cards scroll seamlessly underneath floating sticky bar)
        val context = androidx.compose.ui.platform.LocalContext.current
        val appSettings = remember { com.sloosh.tv.data.repository.AppSettings(context) }
        val gridColumns = appSettings.gridColumns
        val isCompact = gridColumns >= 6
        val gridSpacing = if (isCompact) 12.dp else 16.dp

        AnimatedContent(
            targetState = state.selectedCategory,
            transitionSpec = {
                val isForward = targetState.ordinal > initialState.ordinal
                val direction = if (isForward) 1 else -1
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> (direction * fullWidth * 0.40f).toInt() },
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 260))) togetherWith (slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (-direction * fullWidth * 0.40f).toInt() },
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 180)))
            },
            label = "categoryGalleryTransition",
            modifier = Modifier.fillMaxSize()
        ) { category ->
            val categoryItems = state.categoryItems[category] ?: emptyList()
            val isCurrentLoading = state.isLoading && categoryItems.isEmpty()

            if (isCurrentLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                }
            } else {
                androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid(
                    state = gridState,
                    columns = androidx.tv.foundation.lazy.grid.TvGridCells.Fixed(gridColumns),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    contentPadding = PaddingValues(start = 4.dp, top = 75.dp, end = 16.dp, bottom = 40.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Poster Items
                    items(categoryItems.size, key = { categoryItems[it].identifier }) { index ->
                        val item = categoryItems[index]
                        if (index >= categoryItems.size - 4 && !state.isLoadingMore && state.hasMorePages) {
                            LaunchedEffect(index) {
                                viewModel.loadData(reset = false)
                            }
                        }

                        val cardModifier = if (index < gridColumns) {
                            Modifier
                                .then(if (index == 0) Modifier.focusRequester(firstCardFocusRequester) else Modifier)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                        keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                                    ) {
                                        try {
                                            categoryFocusRequesters[state.selectedCategory.ordinal].requestFocus()
                                            true
                                        } catch (e: Exception) {
                                            false
                                        }
                                    } else false
                                }
                        } else Modifier

                        MediaCard(
                            item = item,
                            onClick = { onMediaSelected(item.identifier) },
                            compact = isCompact,
                            modifier = cardModifier
                        )
                    }

                    if (state.isLoadingMore) {
                        item(span = { androidx.tv.foundation.lazy.grid.TvGridItemSpan(gridColumns) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }

        // Soft cinematic top depth gradient scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF09090A).copy(alpha = 0.88f),
                            0.40f to Color(0xFF09090A).copy(alpha = 0.58f),
                            0.72f to Color(0xFF09090A).copy(alpha = 0.20f),
                            1.00f to Color.Transparent
                        )
                    )
                )
        )

        // Floating Sticky Segmented Category Capsule Bar with Adaptive Text-Width Sliding Pill
        val categories = HomeCategory.values()
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
                .padding(start = 4.dp, top = 15.dp, end = 16.dp, bottom = 20.dp),
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
                                .focusRequester(categoryFocusRequesters[index])
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && state.selectedCategory != cat) {
                                        viewModel.selectCategory(cat)
                                        coroutineScope.launch { gridState.scrollToItem(0) }
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
                                                try {
                                                    firstCardFocusRequester.requestFocus()
                                                    true
                                                } catch (e: Exception) {
                                                    false
                                                }
                                            }
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
                            val textColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isSelected) Color.Black else Color.White.copy(alpha = 0.65f),
                                animationSpec = androidx.compose.animation.core.tween(durationMillis = 160),
                                label = "tabTextColor"
                            )

                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MediaCard(
    item: MediaDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onFocus: (() -> Unit)? = null
) {
    var isCardFocused by remember { mutableStateOf(false) }

    val animatedTitleColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isCardFocused) Color.White else Color.White.copy(alpha = 0.85f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "mediaTitleColor"
    )

    val animatedMetaColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isCardFocused) Color.White.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.48f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "mediaMetaColor"
    )

    val animatedTextScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isCardFocused) 1.04f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "mediaTextScale"
    )

    val animatedTextOffsetY by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isCardFocused) (if (compact) 4.5.dp else 6.dp) else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "mediaTextOffset"
    )

    val cardCornerRadius = if (compact) 14.dp else 16.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        // ─── Poster (True 2:3 aspect ratio, no top/bottom cropping) ─────
        SlooshFocusableCard(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = ContinuousRoundedRectangle(cardCornerRadius),
            focusedScale = 1.08f
        ) { cardFocused ->
            LaunchedEffect(cardFocused) {
                isCardFocused = cardFocused
                if (cardFocused && onFocus != null) onFocus()
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
                    val badgeRadius = if (compact) 6.dp else 7.dp
                    val badgePaddingHorizontal = if (compact) 5.dp else 6.5.dp
                    val badgePaddingVertical = if (compact) 2.dp else 2.5.dp
                    val badgeFontSize = if (compact) 12.sp else 13.5.sp
                    val badgeMargin = if (compact) 6.dp else 8.dp

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(badgeMargin)
                            .clip(ContinuousRoundedRectangle(badgeRadius))
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

        // ─── Title + Meta BELOW (scales, offsets, and highlights in sync) ────────
        val topSpacer = if (compact) 5.5.dp else 7.dp
        val titleSize = if (compact) 13.5.sp else 15.sp
        val titleLineHeight = if (compact) 17.sp else 19.sp
        val metaSize = if (compact) 11.5.sp else 12.5.sp

        Column(
            modifier = Modifier
                .offset(y = animatedTextOffsetY)
                .graphicsLayer {
                    scaleX = animatedTextScale
                    scaleY = animatedTextScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                }
        ) {
            Spacer(modifier = Modifier.height(topSpacer))
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCardFocused) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = titleSize,
                    lineHeight = titleLineHeight
                ),
                color = animatedTitleColor,
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
                    color = animatedMetaColor,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}
