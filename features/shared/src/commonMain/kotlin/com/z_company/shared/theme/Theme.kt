package com.z_company.shared.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

@Composable
fun LocoAppTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appColors: AppColors = remember(isDark) {
        AppColors.appColors(isDark)
    }
    val appShapes: AppShapes = AppShapes.appShapes()
    val appTypography: AppTypography = AppTypography.getType()

    CompositionLocalProvider(
        LocalAppTypography provides appTypography,
        LocalAppColors provides appColors,
        LocalAppShapes provides appShapes,
    ) {
        MaterialTheme(
            colorScheme = appColors.materialColors,
            typography = appTypography.materialTypography,
            shapes = appShapes.materialShapes,
            content = content
        )
    }
}
