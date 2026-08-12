package com.sloosh.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class GradientDirection {
    VERTICAL_BOTTOM_TO_TOP,
    HORIZONTAL_LEFT_TO_RIGHT
}

@Composable
fun ProgressiveGradientOverlay(
    modifier: Modifier = Modifier,
    direction: GradientDirection = GradientDirection.HORIZONTAL_LEFT_TO_RIGHT,
    baseColor: Color = Color.Black
) {
    val brush = when (direction) {
        GradientDirection.HORIZONTAL_LEFT_TO_RIGHT -> {
            Brush.horizontalGradient(
                0.0f to baseColor.copy(alpha = 1.0f),
                0.4f to baseColor.copy(alpha = 0.85f),
                0.7f to baseColor.copy(alpha = 0.45f),
                1.0f to Color.Transparent
            )
        }
        GradientDirection.VERTICAL_BOTTOM_TO_TOP -> {
            Brush.verticalGradient(
                0.0f to Color.Transparent,
                0.3f to baseColor.copy(alpha = 0.45f),
                0.7f to baseColor.copy(alpha = 0.85f),
                1.0f to baseColor.copy(alpha = 1.0f)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    )
}
