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
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.sloosh.tv.ui.theme.BackgroundDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asynchronously extracts the true mathematical average color of the backdrop image (matching iOS CIAreaAverage).
 * Uses Coil's shared singleton ImageLoader with disk & memory cache, and supports instant fallback
 * if the primary high-res backdrop fails or is unavailable.
 */
@Composable
fun rememberAdaptiveAmbientColor(
    primaryUrl: String?,
    fallbackUrl: String? = null,
    defaultColor: Color = BackgroundDark
): State<Color> {
    val context = LocalContext.current
    var extractedColor by remember(primaryUrl, fallbackUrl) { mutableStateOf(defaultColor) }

    LaunchedEffect(primaryUrl, fallbackUrl) {
        val color = withContext(Dispatchers.IO) {
            // 1. Try primary URL (small thumbnail / high-res backdrop)
            if (!primaryUrl.isNullOrEmpty()) {
                val primaryColor = extractAverageBackdropColor(context, primaryUrl)
                if (primaryColor != null) {
                    return@withContext primaryColor
                }
            }
            // 2. Fallback to poster URL if backdrop is missing / 404
            if (!fallbackUrl.isNullOrEmpty()) {
                val fallbackExtracted = extractAverageBackdropColor(context, fallbackUrl)
                if (fallbackExtracted != null) {
                    return@withContext fallbackExtracted
                }
            }
            defaultColor
        }
        extractedColor = color
    }

    return animateColorAsState(
        targetValue = extractedColor,
        animationSpec = tween(durationMillis = 500),
        label = "ambientColorAnimation"
    )
}

/**
 * Computes average RGB across all pixels in the downsampled image (equivalent to iOS CIAreaAverage filter).
 */
private suspend fun extractAverageBackdropColor(
    context: Context,
    imageUrl: String
): Color? {
    return try {
        val loader = Coil.imageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Size(32, 32))
            .allowHardware(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()

        val result = loader.execute(request)
        if (result is SuccessResult) {
            val bitmap = result.drawable.toBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val totalPixels = bitmap.width * bitmap.height
            if (totalPixels <= 0) return null

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

            // Adjust lightness into a deep, luxurious dark theme range for TV (0.06..0.11)
            hsl[2] = hsl[2].coerceIn(0.06f, 0.11f)
            hsl[1] = (hsl[1] * 0.90f).coerceIn(0.12f, 0.70f)

            val adjustedRgb = ColorUtils.HSLToColor(hsl)
            Color(adjustedRgb)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
