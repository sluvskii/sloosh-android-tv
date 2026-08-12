package com.sloosh.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val DarkColorScheme = darkColorScheme(
    primary = SlooshAccentDark,
    onPrimary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark
)

@OptIn(ExperimentalTvMaterial3Api::class)
private val LightColorScheme = lightColorScheme(
    primary = SlooshAccentLight,
    onPrimary = BackgroundLight,
    background = BackgroundLight,
    onBackground = BackgroundDark,
    surface = BackgroundLight,
    onSurface = BackgroundDark
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SlooshTVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
