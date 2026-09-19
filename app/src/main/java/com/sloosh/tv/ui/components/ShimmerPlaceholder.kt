package com.sloosh.tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = remember {
        listOf(
            Color.White.copy(alpha = 0.04f),
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.04f)
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .background(brush)
    )
}

@Composable
fun PosterGridShimmer(
    gridColumns: Int = 5,
    itemCount: Int = 12,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    val horizontalGridSpacing = if (isCompact) 4.dp else 6.dp
    val verticalGridSpacing = if (isCompact) 2.dp else 4.dp
    val skeletonShape = if (isCompact) ContinuousRoundedRectangle(13.dp) else ContinuousRoundedRectangle(16.dp)

    androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid(
        columns = androidx.tv.foundation.lazy.grid.TvGridCells.Fixed(gridColumns),
        horizontalArrangement = Arrangement.spacedBy(horizontalGridSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalGridSpacing),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(itemCount) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(skeletonShape)
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                ShimmerEffect(modifier = Modifier.fillMaxSize(), brush = brush)
            }
        }
    }
}

@Composable
fun ContinueGridShimmer(
    itemCount: Int = 6,
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()

    androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid(
        columns = androidx.tv.foundation.lazy.grid.TvGridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 40.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(itemCount) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(ContinuousRoundedRectangle(18.dp))
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                ShimmerEffect(modifier = Modifier.fillMaxSize(), brush = brush)
            }
        }
    }
}

@Composable
fun CatalogRowShimmer(title: String) {
    val brush = rememberShimmerBrush()
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(24.dp)
                .clip(ContinuousCapsule)
                .background(Color.White.copy(alpha = 0.1f))
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(6) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(210.dp)
                        .clip(ContinuousRoundedRectangle(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    ShimmerEffect(modifier = Modifier.fillMaxSize(), brush = brush)
                }
            }
        }
    }
}

