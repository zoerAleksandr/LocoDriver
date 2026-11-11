package com.z_company.loco_driver.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MashinistColors(
    val bg: Color,
    val bgElevated: Color,
    val bgSubtle: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val accentHover: Color,
    val cta: Color,
    val ctaInk: Color,
    val chipBg: Color,
    val chipBgActive: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
)

val LightMashinistColors = MashinistColors(
    bg = LightBg,
    bgElevated = LightBgElevated,
    bgSubtle = LightBgSubtle,
    surface = LightSurface,
    surfaceAlt = LightSurfaceAlt,
    border = LightBorder,
    borderStrong = LightBorderStrong,
    text = LightText,
    textMuted = LightTextMuted,
    textFaint = LightTextFaint,
    accent = LightAccent,
    accentInk = LightAccentInk,
    accentSoft = LightAccentSoft,
    accentHover = LightAccentHover,
    cta = LightCta,
    ctaInk = LightCtaInk,
    chipBg = LightChipBg,
    chipBgActive = LightChipBgActive,
    success = LightSuccess,
    warning = LightWarning,
    danger = LightDanger,
)

val DarkMashinistColors = MashinistColors(
    bg = DarkBg,
    bgElevated = DarkBgElevated,
    bgSubtle = DarkBgSubtle,
    surface = DarkSurface,
    surfaceAlt = DarkSurfaceAlt,
    border = DarkBorder,
    borderStrong = DarkBorderStrong,
    text = DarkText,
    textMuted = DarkTextMuted,
    textFaint = DarkTextFaint,
    accent = DarkAccent,
    accentInk = DarkAccentInk,
    accentSoft = DarkAccentSoft,
    accentHover = DarkAccentHover,
    cta = DarkCta,
    ctaInk = DarkCtaInk,
    chipBg = DarkChipBg,
    chipBgActive = DarkChipBgActive,
    success = DarkSuccess,
    warning = DarkWarning,
    danger = DarkDanger,
)

// Palette: Кремовая бумага (red)
val CreamLightColors = MashinistColors(
    bg = Color(0xFFE8E4D8),
    bgElevated = Color(0xFFF3F0E6),
    bgSubtle = Color(0xFFDED9CA),
    surface = Color(0xFFF3F0E6),
    surfaceAlt = Color(0xFFEEEADC),
    border = Color(0x141C1A16),
    borderStrong = Color(0x2E1C1A16),
    text = Color(0xFF1C1A16),
    textMuted = Color(0x991C1A16),
    textFaint = Color(0x611C1A16),
    accent = Color(0xFFE2483D),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0x1AE2483D),
    accentHover = Color(0xFFC13A30),
    cta = Color(0xFF1C1A16),
    ctaInk = Color(0xFFFFFFFF),
    chipBg = Color(0xFFEEEADC),
    chipBgActive = Color(0xFFF5D5D2),
    success = Color(0xFF1F8A3F),
    warning = Color(0xFFE08100),
    danger = Color(0xFFE2483D),
)

val CreamDarkColors = MashinistColors(
    bg = Color(0xFF101010),
    bgElevated = Color(0xFF2A2823),
    bgSubtle = Color(0xFF1A1916),
    surface = Color(0xFF2A2823),
    surfaceAlt = Color(0xFF36332C),
    border = Color(0x14E8E4D8),
    borderStrong = Color(0x2EE8E4D8),
    text = Color(0xFFE8E4D8),
    textMuted = Color(0x99E8E4D8),
    textFaint = Color(0x61E8E4D8),
    accent = Color(0xFFFF5A4E),
    accentInk = Color(0xFF121210),
    accentSoft = Color(0x29FF5A4E),
    accentHover = Color(0xFFFF7568),
    cta = Color(0xFFE8E4D8),
    ctaInk = Color(0xFF121210),
    chipBg = Color(0xFF36332C),
    chipBgActive = Color(0xFF44261F),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFFB547),
    danger = Color(0xFFFF5A4E),
)

enum class MashinistPalette(val displayName: String) {
    DEFAULT("По умолчанию"),
    CREAM("Кремовая бумага"),
}

fun getMashinistColors(palette: MashinistPalette, isDark: Boolean): MashinistColors {
    return when (palette) {
        MashinistPalette.DEFAULT -> if (isDark) DarkMashinistColors else LightMashinistColors
        MashinistPalette.CREAM -> if (isDark) CreamDarkColors else CreamLightColors
    }
}

val LocalMashinistColors = staticCompositionLocalOf { LightMashinistColors }

@Immutable
object MashinistSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
}

@Immutable
object MashinistRadius {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 12.dp
    val card: Dp = 16.dp
    val lg: Dp = 18.dp
    val xl: Dp = 24.dp
    val sheet: Dp = 28.dp
    val pill: Dp = 999.dp
}

@Immutable
object MashinistSemantic {
    val rowPadX: Dp = 20.dp
    val rowPadY: Dp = 14.dp
    val rowPadYTouch: Dp = 16.dp
    val rowGap: Dp = 12.dp
    val cardPadX: Dp = 20.dp
    val cardPadY: Dp = 18.dp
    val cardGap: Dp = 12.dp
    val inputMinH: Dp = 44.dp
    val inputPadX: Dp = 14.dp
    val avatarSm: Dp = 28.dp
    val avatarMd: Dp = 32.dp
    val avatarLg: Dp = 36.dp
}
