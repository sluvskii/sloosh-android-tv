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
import kotlin.math.abs
import kotlin.math.min

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

    // Smooth continuous rotating border beam (pure glowing comet on transparent background)
    val infiniteTransition = rememberInfiniteTransition(label = "borderBeamAnimation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beamRotation"
    )

    val beamBrush = remember(rotationAngle) {
        val sampleCount = 24
        val beamArc = 90f // Angular width of the glowing light beam
        val stops = Array(sampleCount + 1) { i ->
            val fraction = i.toFloat() / sampleCount.toFloat()
            val angle = fraction * 360f
            val diff = abs(angle - rotationAngle)
            val dist = min(diff, 360f - diff)

            val intensity = if (dist < beamArc) {
                val norm = dist / beamArc
                (1f - norm * norm).coerceIn(0f, 1f)
            } else 0f

            // Zero base alpha (100% transparent when beam is elsewhere) -> peak 100% pure white at the beam head
            val alpha = (intensity * intensity * 1.0f).coerceIn(0f, 1f)
            fraction to Color.White.copy(alpha = alpha)
        }

        Brush.sweepGradient(*stops)
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
                elevation = 16.dp,
                elevationColor = Color.Black.copy(alpha = 0.65f)
            )
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(0.dp, Color.Transparent), shape = shape),
            focusedBorder = Border(
                border = BorderStroke(2.dp, beamBrush),
                shape = shape
            )
        )
    ) {
        Box {
            content(isFocused)
        }
    }
}
