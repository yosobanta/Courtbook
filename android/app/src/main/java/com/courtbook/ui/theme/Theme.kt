package com.courtbook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = CourtGreen,
        onPrimary = Color.Black,
        primaryContainer = CourtGreenDark,
        onPrimaryContainer = Color.White,
        secondary = AccentAmber,
        onSecondary = Color.Black,
        background = DarkBg,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceElevated,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkBorder)

@Composable
fun CourtBookTheme(content: @Composable () -> Unit) {
  MaterialTheme(
      colorScheme = DarkColorScheme,
      typography = Typography,
      content = content)
}
