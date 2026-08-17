package com.sloosh.tv.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        // Fullscreen Edge-to-Edge Native TV Poster Grid (Cards scroll seamlessly underneath floating sticky bar)
        val context = androidx.compose.ui.platform.LocalContext.current
        val appSettings = remember { com.sloosh.tv.data.repository.AppSettings(context) }
        val gridColumns = appSettings.gridColumns
        val isCompact = gridColumns >= 6
        val gridSpacing = if (isCompact) 12.dp else 16.dp

        if (state.isLoading) {
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
                contentPadding = PaddingValues(start = 4.dp, top = 88.dp, end = 16.dp, bottom = 40.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Poster Items
                items(state.items.size, key = { state.items[it].identifier }) { index ->
                    val item = state.items[index]
                    if (index >= state.items.size - 4 && !state.isLoadingMore && state.hasMorePages) {
                        LaunchedEffect(index) {
                            viewModel.loadData(reset = false)
                        }
                    }

                    val cardModifier = if (index == 0) {
                        Modifier.focusRequester(firstCardFocusRequester)
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

        // Floating Sticky Category Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(start = 4.dp, top = 20.dp, end = 16.dp, bottom = 24.dp)
        ) {
            // Category Tabs only
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeCategory.values().forEach { cat ->
                    SlooshButton(
                        text = cat.title,
                        isPrimary = state.selectedCategory == cat,
                        onClick = {
                            viewModel.selectCategory(cat)
                            coroutineScope.launch { gridState.scrollToItem(0) }
                        },
                        modifier = Modifier
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                    keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                    try {
                                        firstCardFocusRequester.requestFocus()
                                        true
                                    } catch (e: Exception) {
                                        false
                                    }
                                } else false
                            }
                    )
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
