package com.z_company.shared.theme

import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dark palette (matches Android app/theme/Color.kt) ──

private val DarkPrimary = Color(0xFFf0f0f0)
private val DarkOnPrimary = Color(0xFF1A1A1A)
private val DarkSecondary = Color(0xFF515356)
private val DarkTertiary = Color(0xFF92b2e5)
private val DarkSurface = Color(0xFF63666A)
private val DarkOnSurface = Color(0xFFEBE8E8)
private val DarkBackground = Color(0xFF333333)
private val DarkError = Color(0xFFeb9e9e)
private val DarkSecondaryContainer = Color(0xFF363636)
private val DarkSurfaceContainerLow = Color(0xFFE7DEBC)
private val DarkSurfaceContainerHigh = Color(0xFF9D97DC)
private val SurfaceBrightDark = Color(0xFF99918C)
private val SurfaceDimDark = Color(0xFF858FA3)

// ── Light palette ──

private val LightPrimary = Color(0xFF383837)
private val LightOnPrimary = Color(0xFFEFEFEF)
private val LightSecondary = Color(0xFFF6F5EF)
private val LightSurface = Color(0xFFe2e0d8)
private val LightSurfaceVariant = Color(0xFF383837)
private val LightSurfaceTint = Color(0xFFDEDEDB)
private val LightOnSurface = Color(0xFFEFEDE3)
private val LightBackground = Color(0xFFefede3)
private val LightError = Color(0xFFD05C4A)
private val LightSecondaryContainer = Color(0xFFEEEEEE)
private val LightSurfaceContainerLow = Color(0xFF26405c)
private val LightSurfaceContainerHigh = Color(0xFF9D97DC)
private val SurfaceBrightLight = Color(0xFFD6C9C1)
private val SurfaceDimLight = Color(0xFFC0CCE4)

// ── Common ──

private val Blue = Color(0xFF3576FF)
private val OnError = Color(0xFFFFFFFF)

// ── Color Schemes ──

internal val DarkColorPalette =
    AppColors(
        materialColors = darkColorScheme(
            primary = DarkPrimary,
            onPrimary = DarkOnPrimary,
            secondary = DarkSecondary,
            secondaryContainer = DarkSecondaryContainer,
            tertiary = DarkTertiary,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            background = DarkBackground,
            error = DarkError,
            onError = OnError,
            surfaceBright = SurfaceBrightDark,
            surfaceDim = SurfaceDimDark,
            surfaceContainerLow = DarkSurfaceContainerLow,
            surfaceContainerHigh = DarkSurfaceContainerHigh,
        ),
        someCustomColor = Color.Red,
    )

internal val LightColorPalette =
    AppColors(
        materialColors = lightColorScheme(
            primary = LightPrimary,
            onPrimary = LightOnPrimary,
            secondary = LightSecondary,
            secondaryContainer = LightSecondaryContainer,
            tertiary = Blue,
            surface = LightSurface,
            surfaceVariant = LightSurfaceVariant,
            surfaceTint = LightSurfaceTint,
            onSurface = LightOnSurface,
            background = LightBackground,
            error = LightError,
            onError = OnError,
            surfaceBright = SurfaceBrightLight,
            surfaceDim = SurfaceDimLight,
            surfaceContainerLow = LightSurfaceContainerLow,
            surfaceContainerHigh = LightSurfaceContainerHigh,
        ),
        someCustomColor = Color.Blue,
    )

@Composable
fun transparentColorForTextField() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)
