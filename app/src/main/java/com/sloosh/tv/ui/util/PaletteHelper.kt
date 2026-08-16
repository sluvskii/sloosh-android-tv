package com.sloosh.tv.ui.util

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.sloosh.tv.ui.theme.BackgroundDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asynchronously extracts the true mathematical average color of the backdrop image (matching iOS CIAreaAverage).
 * It preserves the exact atmospheric tone of the scene (e.g. dark slate/steel city for Spider-Man)
 * and adjusts lightness into a deep, luxurious dark theme range for perfect text readability.
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
            extractAverageBackdropColor(context, imageUrl, defaultColor)
        }
        extractedColor = color
    }

    return animateColorAsState(
        targetValue = extractedColor,
        animationSpec = tween(durationMillis = 600),
        label = "ambientColorAnimation"
    )
}

/**
 * Computes average RGB across all pixels in the image (equivalent to iOS CIAreaAverage filter).
 */
private suspend fun extractAverageBackdropColor(
    context: Context,
    imageUrl: String,
    fallback: Color
): Color {
    return try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Size(32, 32))
            .allowHardware(false)
            .build()

        val result = loader.execute(request)
        if (result is SuccessResult) {
            val bitmap = result.drawable.toBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val totalPixels = bitmap.width * bitmap.height
            val pixels = IntArray(totalPixels)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            var totalR = 0L
            var totalG = 0L
            var totalB = 0L

            for (pixel in pixels) {
                totalR += (pixel shr 16) and 0xFF
                totalG += (pixel shr 8) and 0xFF
                totalB += pixel and 0xFF
            }

            val avgR = (totalR / totalPixels).toInt()
            val avgG = (totalG / totalPixels).toInt()
            val avgB = (totalB / totalPixels).toInt()

            val hsl = FloatArray(3)
            ColorUtils.RGBToHSL(avgR, avgG, avgB, hsl)

            // Adjust lightness to a deep dark level for movie TV interface (0.07..0.12)
            hsl[2] = hsl[2].coerceIn(0.06f, 0.11f)
            hsl[1] = (hsl[1] * 0.90f).coerceIn(0.12f, 0.70f)

            val adjustedRgb = ColorUtils.HSLToColor(hsl)
            Color(adjustedRgb)
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}
