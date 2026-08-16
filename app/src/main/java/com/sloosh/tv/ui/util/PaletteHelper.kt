package com.sloosh.tv.ui.util

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.sloosh.tv.ui.theme.BackgroundDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asynchronously extracts an adaptive, cinematic ambient color from an image URL (poster or backdrop).
 * The color is tuned for maximum readability (deep luminosity and balanced saturation)
 * and smoothly animated using [animateColorAsState].
 */
@Composable
fun rememberAdaptiveAmbientColor(
    imageUrl: String?,
    defaultColor: Color = BackgroundDark
): State<Color> {
    val context = LocalContext.current
    var extractedColor by remember(imageUrl) { mutableStateOf(defaultColor) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) {
            extractedColor = defaultColor
            return@LaunchedEffect
        }

        val color = withContext(Dispatchers.IO) {
            extractAmbientColor(context, imageUrl, defaultColor)
        }
        extractedColor = color
    }

    return animateColorAsState(
        targetValue = extractedColor,
        animationSpec = tween(durationMillis = 650),
        label = "ambientColorAnimation"
    )
}

/**
 * Decodes a tiny 96x96 thumbnail of the image and runs Palette on it off the main thread.
 */
private suspend fun extractAmbientColor(
    context: Context,
    imageUrl: String,
    fallback: Color
): Color {
    return try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Size(96, 96))
            .allowHardware(false)
            .build()

        val result = loader.execute(request)
        if (result is SuccessResult) {
            val bitmap = result.drawable.toBitmap(96, 96, Bitmap.Config.ARGB_8888)
            val palette = Palette.from(bitmap).maximumColorCount(16).generate()

            // Prioritize vibrant and rich swatches
            val swatch = palette.darkVibrantSwatch
                ?: palette.vibrantSwatch
                ?: palette.darkMutedSwatch
                ?: palette.mutedSwatch
                ?: palette.dominantSwatch

            if (swatch != null) {
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(swatch.rgb, hsl)

                // Tune for deep, luxurious iOS/tvOS-style ambient tone:
                // Keep the original hue, soften saturation slightly, constrain lightness to deep rich range (0.07..0.15)
                hsl[1] = (hsl[1] * 0.85f).coerceIn(0.25f, 0.85f)
                hsl[2] = hsl[2].coerceIn(0.08f, 0.16f)

                val adjustedRgb = ColorUtils.HSLToColor(hsl)
                Color(adjustedRgb)
            } else {
                fallback
            }
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}
