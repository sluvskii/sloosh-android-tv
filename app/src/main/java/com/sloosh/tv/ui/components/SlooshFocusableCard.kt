package com.sloosh.tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun SlooshFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ContinuousRoundedRectangle(18.dp),
    focusedScale: Float = 1.08f,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Smooth continuous rotating border beam (aura comet circling around the card perimeter)
    val infiniteTransition = rememberInfiniteTransition(label = "borderBeamAnimation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beamRotation"
    )

    val beamBrush = remember(rotationAngle) {
        val shift = (rotationAngle / 360f) % 1f
        val rawStops = listOf(
            0.00f to Color.Transparent,
            0.45f to Color.Transparent,
            0.65f to Color.White.copy(alpha = 0.20f),
            0.80f to Color.White.copy(alpha = 0.65f),
            0.92f to Color.White,
            0.98f to Color.White.copy(alpha = 0.70f),
            1.00f to Color.Transparent
        )
        val shifted = rawStops.map { (stop, color) ->
            ((stop + shift) % 1.0f) to color
        }.sortedBy { it.first }

        Brush.sweepGradient(*shifted.toTypedArray())
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        scale = CardDefaults.scale(focusedScale = focusedScale),
        glow = CardDefaults.glow(
            glow = androidx.tv.material3.Glow.None,
            focusedGlow = androidx.tv.material3.Glow(
                elevation = 18.dp,
                elevationColor = Color.White.copy(alpha = 0.22f)
            )
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(0.dp, Color.Transparent), shape = shape),
            focusedBorder = Border(
                border = BorderStroke(2.5.dp, beamBrush),
                shape = shape
            )
        )
    ) {
        Box {
            content(isFocused)
        }
    }
}
