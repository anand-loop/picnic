// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF5A5A5A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF7A7A7A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2F2F2),
    onTertiaryContainer = Color(0xFF1A1A1A),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF5A5A5A),
    surfaceTint = Color(0xFF1A1A1A),
    outline = Color(0xFFB5B5B5),
    outlineVariant = Color(0xFFDEDEDE),
    error = Color(0xFF424242),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFE5E5E5),
    onErrorContainer = Color(0xFF1A1A1A),
    inverseSurface = Color(0xFF1A1A1A),
    inverseOnSurface = Color(0xFFF2F2F2),
    inversePrimary = Color(0xFFF2F2F2),
    scrim = Color(0xFF000000)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF2F2F2),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFF2F2F2),
    secondary = Color(0xFFB5B5B5),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFF2F2F2),
    tertiary = Color(0xFF858585),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF1A1A1A),
    onTertiaryContainer = Color(0xFFF2F2F2),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFA5A5A5),
    surfaceTint = Color(0xFFF2F2F2),
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF2A2A2A),
    error = Color(0xFFB5B5B5),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF2A2A2A),
    onErrorContainer = Color(0xFFF2F2F2),
    inverseSurface = Color(0xFFF2F2F2),
    inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFF1A1A1A),
    scrim = Color(0xFF000000)
)

@Composable
fun PicnicTheme(themePreference: ThemePreference = ThemePreference.SYSTEM, content: @Composable () -> Unit) {
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = PicnicTypography,
        content = content
    )
}
