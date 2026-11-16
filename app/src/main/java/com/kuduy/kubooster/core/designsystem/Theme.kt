package com.kuduy.kubooster.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * KuBooster theme. Dark is the designed default (the spec's palette); a light
 * scheme exists because the OS may force light mode — fully mapped, light
 * surfaces included.
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

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF10265F),
    secondary = KubeColors.Blue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EDFB),
    onSecondaryContainer = Color(0xFF12294D),
    background = Color(0xFFF7F9F3),
    onBackground = Color(0xFF14181F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14181F),
    surfaceVariant = Color(0xFFE9EDF2),
    onSurfaceVariant = Color(0xFF515A68),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFBF7),
    surfaceContainer = Color(0xFFF5F7F1),
    surfaceContainerHigh = Color(0xFFF0F3EB),
    surfaceContainerHighest = Color(0xFFEAEEE6),
    outline = Color(0xFFD4DAE2),
    outlineVariant = Color(0xFFD4DAE2),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFBE9E7),
    onErrorContainer = Color(0xFF8C1D18),
    scrim = Color(0xB314181F),
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
