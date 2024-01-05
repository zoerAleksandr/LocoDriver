package com.example.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.example.core.ui.theme.custom.AppColors
import com.example.core.ui.theme.custom.AppColors.Companion.appColors
import com.example.core.ui.theme.custom.AppShapes
import com.example.core.ui.theme.custom.AppShapes.Companion.appShapes
import com.example.core.ui.theme.custom.AppTypography
import com.example.core.ui.theme.custom.AppTypography.Companion.getType
import com.example.core.ui.theme.custom.LocalAppColors
import com.example.core.ui.theme.custom.LocalAppShapes
import com.example.core.ui.theme.custom.LocalAppTypography

@Composable
fun LocoAppTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appColors: AppColors = remember(isDark) {
        appColors(isDark)
    }
    val appShapes: AppShapes = appShapes()
    val appTypography: AppTypography = getType()

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

