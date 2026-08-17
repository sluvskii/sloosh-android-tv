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
import kotlin.math.sin

/**
 * Universal Focusable Card with true physical perimeter light beam contour.
 * - 100% path-distance based intensity (zero angle distortion or non-linear speed changes).
 * - Perfectly symmetric thin-thick-thin optical pulse via sin(t * PI).
 * - Seamless Butt joins for uninterrupted ribbon stroke with zero overlapping dots.
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

    // Constant physical velocity perimeter progress 0f..1f
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
        val segmentPath = remember { Path() }

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
                        val beamFraction = 0.32f // Symmetrical pulse is ~32% of total perimeter
                        val beamLen = totalLength * beamFraction
                        val halfLen = beamLen / 2f
                        val centerDist = beamProgress * totalLength
                        val startDist = centerDist - halfLen

                        // Subdivide the beam into 20 seamless slices along the perimeter path
                        val subSteps = 20
                        val stepLen = beamLen / subSteps
                        val strokeWidthPx = 1.5.dp.toPx()

                        for (i in 0 until subSteps) {
                            val subStart = startDist + i * stepLen
                            val subEnd = startDist + (i + 1) * stepLen

                            // Normalized position along beam [0..1]
                            val t = (i + 0.5f) / subSteps.toFloat()

                            // Perfectly symmetric sine bell curve (0 at start -> 1.0 in center -> 0 at end)
                            val intensity = sin(t * Math.PI).toFloat().coerceIn(0f, 1f)
                            val stepAlpha = (intensity * intensity * 0.50f * focusAlpha).coerceIn(0f, 1f)

                            // Use Round cap strictly on the outer tips, Butt cap on internal segments (zero dots/overlaps)
                            val cap = when (i) {
                                0, subSteps - 1 -> StrokeCap.Round
                                else -> StrokeCap.Butt
                            }

                            segmentPath.reset()
                            extractLoopSegment(pathMeasure, totalLength, subStart, subEnd, segmentPath)

                            drawPath(
                                path = segmentPath,
                                color = Color.White,
                                alpha = stepAlpha,
                                style = Stroke(
                                    width = strokeWidthPx,
                                    cap = cap,
                                    join = StrokeJoin.Round
                                ),
                                blendMode = BlendMode.Plus
                            )
                        }
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
