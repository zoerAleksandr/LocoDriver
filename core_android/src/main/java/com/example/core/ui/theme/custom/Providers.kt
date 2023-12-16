package com.example.core.ui.theme.custom

import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalAppTypography = staticCompositionLocalOf {
    AppTypography.appTypography()
}

internal val LocalAppColors = staticCompositionLocalOf {
    AppColors.appColors(
        darkTheme = false
    )
}

internal val LocalAppShapes = staticCompositionLocalOf {
    AppShapes.appShapes()
}
