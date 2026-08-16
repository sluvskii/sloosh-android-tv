package com.sloosh.tv.ui.theme

import androidx.compose.ui.graphics.Color

val BackgroundDark = Color(0xFF050505) // Near-black, richer than pure black
val BackgroundLight = Color(0xFFFFFFFF)

// iOS Sloosh accent green (#A3FF5E)
val SlooshGreen = Color(0xFFA3FF5E)
val SlooshGreenDim = Color(0x80A3FF5E) // 50% opacity
val SlooshGreenDark = Color(0xFF6ECF2B)

// Keep white accent for TV-specific focus elements
val SlooshAccentDark = Color(0xFFFFFFFF)
val SlooshAccentLight = Color(0xFF000000)

val SurfaceDark = Color(0xFF111111)
val SurfaceVariantDark = Color(0xFF1C1C1C)

val GlassSurfaceDark = Color(0xFF1A1A1A)
val GlassSurfaceFocusedDark = Color(0xFF252525)
val GlassSurface60 = Color(0x99151515)

val GlassBorderFocusedDark = Color(0xFFFFFFFF)
val GlassBorderUnfocusedDark = Color(0xFF2A2A2A)

// Rating colors matching iOS Sloosh
val RatingGreen = Color(0xFFA3FF5E)
val RatingGreenText = Color(0xFF1A3A00)
val RatingGold = Color(0xFFFFD700)

val RatingIosGreen = Color(0xFF1CB54B) // Juicy vibrant iOS green
val RatingIosGray = Color(0xFF6B7280)  // Balanced gray
val RatingIosRed = Color(0xFFE11D48)   // Vivid red

fun ratingColor(rating: Double): Color = when {
    rating >= 7.0 -> RatingIosGreen
    rating >= 5.0 -> RatingIosGray
    else -> RatingIosRed
}

val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xB3FFFFFF)
val TextMutedDark = Color(0x66FFFFFF)
val TextPlaceholderDark = Color(0x40FFFFFF)



