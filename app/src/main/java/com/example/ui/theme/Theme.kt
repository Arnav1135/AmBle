package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalGlassPreset = staticCompositionLocalOf { GlassPreset.ArcticBlue }

@Composable
fun ChatWaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    glassPreset: GlassPreset = GlassPreset.ArcticBlue,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = glassPreset.primary,
            onPrimary = Color.White,
            primaryContainer = glassPreset.sky,
            onPrimaryContainer = glassPreset.ink,
            secondary = glassPreset.sky,
            onSecondary = glassPreset.ink,
            background = glassPreset.backgroundStart,
            onBackground = Color.White,
            surface = glassPreset.glassBg,
            onSurface = Color.White,
            surfaceVariant = glassPreset.pale2,
            onSurfaceVariant = glassPreset.inkSoft,
            error = WavePayAlert
        )
    } else {
        lightColorScheme(
            primary = glassPreset.primary,
            onPrimary = Color.White,
            primaryContainer = glassPreset.sky,
            onPrimaryContainer = glassPreset.ink,
            secondary = glassPreset.sky,
            onSecondary = glassPreset.ink,
            background = glassPreset.backgroundStart,
            onBackground = glassPreset.ink,
            surface = glassPreset.glassBg,
            onSurface = glassPreset.ink,
            surfaceVariant = glassPreset.pale2,
            onSurfaceVariant = glassPreset.inkSoft,
            error = WavePayAlert
        )
    }

    CompositionLocalProvider(LocalGlassPreset provides glassPreset) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

