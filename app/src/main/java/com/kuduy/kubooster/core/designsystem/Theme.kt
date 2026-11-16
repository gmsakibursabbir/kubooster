package com.kuduy.kubooster.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * KuBooster theme. Dark is the designed default (the spec's palette); a light
 * scheme exists only because the OS may force light mode — it is a mapped
 * inversion, not a separate design.
 */
private val DarkScheme = darkColorScheme(
    primary = KubeColors.Lime,
    onPrimary = KubeColors.OnLime,
    primaryContainer = KubeColors.SurfaceHigh,
    onPrimaryContainer = KubeColors.TextPrimary,
    secondary = KubeColors.Blue,
    onSecondary = Color.White,
    secondaryContainer = KubeColors.Surface,
    onSecondaryContainer = KubeColors.TextPrimary,
    background = KubeColors.Background,
    onBackground = KubeColors.TextPrimary,
    surface = KubeColors.Surface,
    onSurface = KubeColors.TextPrimary,
    surfaceVariant = KubeColors.SurfaceHigh,
    onSurfaceVariant = KubeColors.TextSecondary,
    surfaceContainerLowest = KubeColors.Background,
    surfaceContainerLow = KubeColors.Surface,
    surfaceContainer = KubeColors.Surface,
    surfaceContainerHigh = KubeColors.SurfaceHigh,
    surfaceContainerHighest = KubeColors.SurfaceHigh,
    outline = KubeColors.Border,
    outlineVariant = KubeColors.Border,
    error = KubeColors.Danger,
    onError = Color(0xFF2A0B07),
    errorContainer = Color(0xFF3A1510),
    onErrorContainer = KubeColors.Danger,
    scrim = Color(0xB3090B10),
)

private val LightScheme = darkColorScheme(
    primary = Color(0xFF4C7A00),
    onPrimary = Color.White,
    background = Color(0xFFF7F9F3),
    onBackground = Color(0xFF14181F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14181F),
    surfaceVariant = Color(0xFFE9EDF2),
    onSurfaceVariant = Color(0xFF515A68),
    outline = Color(0xFFD4DAE2),
    outlineVariant = Color(0xFFD4DAE2),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

@Composable
fun KuBoosterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = KubeTypography,
        content = content,
    )
}
