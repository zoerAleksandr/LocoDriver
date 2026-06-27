package com.z_company.loco_driver.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightAccentInk,
    primaryContainer = LightAccentSoft,
    secondary = LightCta,
    onSecondary = LightCtaInk,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorderStrong,
    outlineVariant = LightBorder,
    error = LightDanger,
    onError = OnError,
    tertiary = LightSuccess,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkAccentInk,
    primaryContainer = DarkAccentSoft,
    secondary = DarkCta,
    onSecondary = DarkCtaInk,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkBorderStrong,
    outlineVariant = DarkBorder,
    error = DarkDanger,
    onError = OnError,
    tertiary = DarkSuccess,
)

object MashinistTheme {
    val colors: MashinistColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMashinistColors.current
}

@Composable
fun LocoDriverTheme(
    dynamicColor: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val mashinistColors = if (darkTheme) DarkMashinistColors else LightMashinistColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalMashinistColors provides mashinistColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
