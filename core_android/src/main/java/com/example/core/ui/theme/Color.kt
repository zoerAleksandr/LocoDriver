package com.example.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.core.ui.theme.custom.AppColors

internal val DarkColorPalette =
    AppColors(
        materialColors = darkColorScheme(),
        someCustomColor = Color.Red,
    )

internal val LightColorPalette =
    AppColors(
        materialColors = lightColorScheme(),
        someCustomColor = Color.Blue,
    )