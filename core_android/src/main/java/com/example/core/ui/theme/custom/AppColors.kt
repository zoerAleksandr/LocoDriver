package com.example.core.ui.theme.custom

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.example.core.ui.theme.DarkColorPalette
import com.example.core.ui.theme.LightColorPalette

@Stable
data class AppColors(
    val materialColors: ColorScheme,
    val someCustomColor: Color,
) {
    val primary: Color
        get() = materialColors.primary

    val inversePrimary: Color
        get() = materialColors.inversePrimary

    val secondary: Color
        get() = materialColors.secondary

    val tertiary: Color
        get() = materialColors.tertiary

    val onTertiary: Color
        get() = materialColors.onTertiary

    val background: Color
        get() = materialColors.background

    val surface: Color
        get() = materialColors.surface

    val error: Color
        get() = materialColors.error

    val onPrimary: Color
        get() = materialColors.onPrimary

    val onSecondary: Color
        get() = materialColors.onSecondary

    val onBackground: Color
        get() = materialColors.onBackground

    val onSurface: Color
        get() = materialColors.onSurface

    val onError: Color
        get() = materialColors.onError

    companion object {
        fun appColors(darkTheme: Boolean): AppColors {
            return if (darkTheme) {
                DarkColorPalette
            } else {
                LightColorPalette
            }
        }
    }
}