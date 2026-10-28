package com.courtbook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
    lightColorScheme(
        primary = ForestGreen,
        onPrimary = CardSurface,
        primaryContainer = SoftGreenSurface,
        onPrimaryContainer = ForestGreen,
        secondary = ElectricLime,
        onSecondary = ForestGreen,
        background = WarmBg,
        onBackground = TextDeep,
        surface = CardSurface,
        onSurface = TextDeep,
        surfaceVariant = SoftGreenSurface,
        onSurfaceVariant = TextMuted,
        outline = BorderSubtle
    )

@Composable
fun CourtBookTheme(content: @Composable () -> Unit) {
  MaterialTheme(
      colorScheme = LightColorScheme,
      typography = Typography,
      content = content
  )
}
