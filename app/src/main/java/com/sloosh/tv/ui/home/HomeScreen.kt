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
import com.sloosh.tv.data.db.ProgressEntity
import androidx.compose.ui.graphics.Brush
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
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SlooshGreen, modifier = Modifier.size(48.dp))
            }
        } else {
            androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid(
                state = gridState,
                columns = androidx.tv.foundation.lazy.grid.TvGridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(start = 24.dp, top = 88.dp, end = 24.dp, bottom = 40.dp),
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
                        modifier = cardModifier
                    )
                }

                if (state.isLoadingMore) {
                    item(span = { androidx.tv.foundation.lazy.grid.TvGridItemSpan(5) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SlooshGreen, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }

        // Floating Sticky Category Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 24.dp)
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
    onFocus: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // ─── Poster (True 2:3 aspect ratio, no top/bottom cropping) ─────
        SlooshFocusableCard(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = ContinuousRoundedRectangle(16.dp)
        ) { cardFocused ->
            if (onFocus != null) {
                LaunchedEffect(cardFocused) {
                    if (cardFocused) onFocus()
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.getDisplayPosterUrl(),
                    contentDescription = item.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Adaptive rating badge top-left (iOS style: Green >= 7.0, Gray 5.0-7.0, Red < 5.0)
                if (item.rating != null && item.rating > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(ContinuousRoundedRectangle(7.dp))
                            .background(ratingColor(item.rating))
                            .padding(horizontal = 6.5.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", item.rating),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ─── Title + Meta BELOW (does NOT scale or take focus) ────────
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.displayTitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 19.sp
            ),
            color = Color.White.copy(alpha = 0.95f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        val genreText = item.genres?.firstOrNull()?.name
        val metaText = listOfNotNull(item.yearString.ifEmpty { null }, genreText).joinToString(" • ")
        if (metaText.isNotEmpty()) {
            Text(
                text = metaText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.52f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}


