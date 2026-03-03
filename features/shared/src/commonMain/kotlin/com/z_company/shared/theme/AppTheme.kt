package com.z_company.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

object AppTheme {
    val typography: AppTypography
        @Composable @ReadOnlyComposable
        get() = LocalAppTypography.current

    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current

    val shapes: AppShapes
        @Composable @ReadOnlyComposable
        get() = LocalAppShapes.current
}
