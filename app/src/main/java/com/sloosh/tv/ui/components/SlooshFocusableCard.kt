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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min

/**
 * Universal Focusable Card with uniform-perimeter crisp light beam contour.
 * - Uniform physical velocity along any shape perimeter via PathMeasure.
 * - Single continuous path stroke (zero dots or seam artifacts).
 * - Smooth Fade-In and Fade-Out on focus transition.
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

    // Smooth Fade-In and Fade-Out transition
    val focusAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "beamFocusAlpha"
    )

    // Uniform perimeter progress 0f..1f (continuous constant speed)
    val infiniteTransition = rememberInfiniteTransition(label = "borderBeamAnimation")
    val beamProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beamProgress"
    )

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
        val pathMeasure = remember { PathMeasure() }
        val sourcePath = remember { Path() }
        val cometPath = remember { Path() }

        Box(
            modifier = Modifier.drawWithContent {
                drawContent()

                if (focusAlpha > 0.005f) {
                    val outline = shape.createOutline(size, layoutDirection, this)

                    // Convert any Outline (Generic, Rounded, Rect) to a closed Path
                    sourcePath.reset()
                    when (outline) {
                        is Outline.Rectangle -> sourcePath.addRect(outline.rect)
                        is Outline.Rounded -> sourcePath.addRoundRect(outline.roundRect)
                        is Outline.Generic -> sourcePath.addPath(outline.path)
                    }

                    pathMeasure.setPath(sourcePath, forceClosed = true)
                    val totalLength = pathMeasure.length

                    if (totalLength > 0f) {
                        val beamFraction = 0.30f // Comet length is ~30% of total perimeter
                        val beamLen = totalLength * beamFraction
                        val headDist = beamProgress * totalLength
                        val tailDist = headDist - beamLen

                        // Extract ONE single continuous comet path segment (zero chops, zero dots)
                        cometPath.reset()
                        extractLoopSegment(pathMeasure, totalLength, tailDist, headDist, cometPath)

                        // Calculate head position and angle for smooth luminous sweep gradient
                        val normalizedHead = ((headDist % totalLength) + totalLength) % totalLength
                        val headPos = pathMeasure.getPosition(normalizedHead)
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val headAngle = (Math.toDegrees(
                            atan2((headPos.y - centerY).toDouble(), (headPos.x - centerX).toDouble())
                        ).toFloat() + 360f) % 360f

                        val sampleCount = 24
                        val beamArcDeg = 110f
                        val stops = Array(sampleCount + 1) { i ->
                            val fraction = i.toFloat() / sampleCount.toFloat()
                            val angle = fraction * 360f
                            val diff = abs(angle - headAngle)
                            val dist = min(diff, 360f - diff)

                            val intensity = if (dist < beamArcDeg) {
                                val norm = dist / beamArcDeg
                                cos(norm * (Math.PI / 2.0)).toFloat().coerceIn(0f, 1f)
                            } else 0f

                            val alpha = (intensity * intensity * 0.85f * focusAlpha).coerceIn(0f, 1f)
                            fraction to Color.White.copy(alpha = alpha)
                        }

                        val brush = Brush.sweepGradient(*stops)

                        // SINGLE drawPath call: ZERO intermediate dots, ZERO seams, 100% solid and fluid
                        drawPath(
                            path = cometPath,
                            brush = brush,
                            style = Stroke(
                                width = 1.8.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            ),
                            blendMode = BlendMode.Plus
                        )
                    }
                }
            }
        ) {
            content(isFocused)
        }
    }
}

/**
 * Extracts a segment from a closed looping path with length wrapping into a single destination path.
 */
private fun extractLoopSegment(
    pathMeasure: PathMeasure,
    totalLength: Float,
    rawStart: Float,
    rawEnd: Float,
    destination: Path
) {
    var start = rawStart
    var end = rawEnd

    // Normalize into [0, totalLength)
    while (start < 0f && end < 0f) {
        start += totalLength
        end += totalLength
    }
    while (start >= totalLength && end >= totalLength) {
        start -= totalLength
        end -= totalLength
    }

    if (start < 0f && end >= 0f) {
        // Wraps over start: tail is at end of path, head is at beginning
        pathMeasure.getSegment(start + totalLength, totalLength, destination, startWithMoveTo = true)
        pathMeasure.getSegment(0f, end, destination, startWithMoveTo = true)
    } else if (start < totalLength && end > totalLength) {
        // Wraps over end
        pathMeasure.getSegment(start, totalLength, destination, startWithMoveTo = true)
        pathMeasure.getSegment(0f, end - totalLength, destination, startWithMoveTo = true)
    } else {
        // Standard contiguous segment within [0, totalLength]
        val clampedStart = start.coerceIn(0f, totalLength)
        val clampedEnd = end.coerceIn(0f, totalLength)
        if (clampedEnd > clampedStart) {
            pathMeasure.getSegment(clampedStart, clampedEnd, destination, startWithMoveTo = true)
        }
    }
}
