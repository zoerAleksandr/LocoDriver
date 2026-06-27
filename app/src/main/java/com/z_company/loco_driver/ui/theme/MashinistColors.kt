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
