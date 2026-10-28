package com.courtbook.ui.theme

import androidx.compose.ui.graphics.Color

// --- TARGET PALETTE TOKENS ---
val WarmBg = Color(0xFFF7F8F2)
val CardSurface = Color(0xFFFFFFFF)
val ElectricLime = Color(0xFFD8FF55)
val ForestGreen = Color(0xFF193B27)
val ForestGreenDark = Color(0xFF0F2418)
val SoftGreenSurface = Color(0xFFEAF1E6)
val TextDeep = Color(0xFF202B22)
val TextMuted = Color(0xFF68746A)
val BorderSubtle = Color(0xFFE5E9DE)

val StatusSuccess = Color(0xFF15803D)
val StatusWarning = Color(0xFFD97706)
val StatusDestructive = Color(0xFFDC2626)
val StatusUnavailable = Color(0xFF9CA3AF)
val StatusBookedBg = Color(0xFFFEE2E2)

// --- BRAND & COMPATIBILITY ALIASES ---
// Primary Accent & Brand
val CourtGreen = ForestGreen
val CourtGreenDark = ForestGreenDark
val CourtGreenLight = ElectricLime

// Secondary Electric Accent
val AccentAmber = StatusWarning
val AccentOrange = Color(0xFFEA580C)
val AccentBlue = Color(0xFF2563EB)

// Surfaces (Mapped to fresh warm off-white & white cards)
val DarkBg = WarmBg
val DarkSurface = CardSurface
val DarkSurfaceElevated = CardSurface
val DarkTextPrimary = TextDeep
val DarkTextSecondary = TextMuted
val DarkBorder = BorderSubtle

// Status Colors
val StatusAvailable = StatusSuccess
val StatusHeld = StatusWarning
val StatusBooked = StatusDestructive
val StatusBlocked = StatusUnavailable

// Badges & Chips
val ChipSelectedBg = ForestGreen
val ChipUnselectedBg = SoftGreenSurface
