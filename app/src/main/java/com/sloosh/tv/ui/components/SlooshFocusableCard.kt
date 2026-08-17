package com.sloosh.tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawOutline
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlin.math.abs
import kotlin.math.min

/**
 * Universal Focusable Card with shape-adaptive plus-lighter rotating light beam contour.
 * Dynamically conforms to ANY Shape (CircleShape, ContinuousCapsule, ContinuousRoundedRectangle).
 */
@Composable
fun SlooshFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ContinuousRoundedRectangle(18.dp),
    focusedScale: Float = 1.05f,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Smooth continuous rotating border beam with 360 seamless interpolation
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
        val beamArc = 95f // Angular width of the glowing light beam
        val stops = Array(sampleCount + 1) { i ->
            val fraction = i.toFloat() / sampleCount.toFloat()
            val angle = fraction * 360f
            val diff = abs(angle - rotationAngle)
            val dist = min(diff, 360f - diff)

            val intensity = if (dist < beamArc) {
                val norm = dist / beamArc
                (1f - norm * norm).coerceIn(0f, 1f)
            } else 0f

            // Softened alpha curve for natural additive plus-lighter sheen
            val alpha = (intensity * intensity * 0.55f).coerceIn(0f, 1f)
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
            focusedGlow = androidx.tv.material3.Glow.None
        ),
        border = CardDefaults.border(
            border = Border(border = BorderStroke(0.dp, Color.Transparent), shape = shape),
            focusedBorder = Border(border = BorderStroke(0.dp, Color.Transparent), shape = shape)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    if (isFocused) {
                        val outline = shape.createOutline(size, layoutDirection, this)

                        // 1. Soft glowing bloom pass with BlendMode.Plus adapted to EXACT shape
                        drawOutline(
                            outline = outline,
                            brush = beamBrush,
                            style = Stroke(width = 3.5.dp.toPx()),
                            blendMode = BlendMode.Plus,
                            alpha = 0.35f
                        )

                        // 2. Focused core light beam with BlendMode.Plus adapted to EXACT shape
                        drawOutline(
                            outline = outline,
                            brush = beamBrush,
                            style = Stroke(width = 1.6.dp.toPx()),
                            blendMode = BlendMode.Plus,
                            alpha = 0.60f
                        )
                    }
                }
        ) {
            content(isFocused)
        }
    }
}
