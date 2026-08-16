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
    VERTICAL_TOP_TO_BOTTOM,
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
                0.35f to baseColor.copy(alpha = 0.75f),
                0.70f to baseColor.copy(alpha = 0.35f),
                1.0f to Color.Transparent
            )
        }
        GradientDirection.VERTICAL_BOTTOM_TO_TOP -> {
            Brush.verticalGradient(
                0.0f to Color.Transparent,
                0.35f to baseColor.copy(alpha = 0.35f),
                0.70f to baseColor.copy(alpha = 0.75f),
                1.0f to baseColor.copy(alpha = 1.0f)
            )
        }
        GradientDirection.VERTICAL_TOP_TO_BOTTOM -> {
            Brush.verticalGradient(
                0.0f to baseColor.copy(alpha = 0.80f),
                0.30f to baseColor.copy(alpha = 0.50f),
                0.60f to baseColor.copy(alpha = 0.22f),
                0.85f to baseColor.copy(alpha = 0.05f),
                1.0f to Color.Transparent
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    )
}
