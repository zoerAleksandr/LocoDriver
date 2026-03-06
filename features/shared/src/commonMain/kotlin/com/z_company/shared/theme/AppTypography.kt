package com.z_company.shared.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SharedTypography = Typography(
    titleMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
    ),
)

data class AppTypography(
    val materialTypography: Typography
) {
    val titleLarge: TextStyle get() = materialTypography.titleLarge
    val titleMedium: TextStyle get() = materialTypography.titleMedium
    val titleSmall: TextStyle get() = materialTypography.titleSmall
    val displayLarge: TextStyle get() = materialTypography.displayLarge
    val displayMedium: TextStyle get() = materialTypography.displayMedium
    val displaySmall: TextStyle get() = materialTypography.displaySmall
    val headlineSmall: TextStyle get() = materialTypography.headlineSmall
    val headlineMedium: TextStyle get() = materialTypography.headlineMedium
    val headlineLarge: TextStyle get() = materialTypography.headlineLarge
    val labelSmall: TextStyle get() = materialTypography.labelSmall
    val labelMedium: TextStyle get() = materialTypography.labelMedium
    val labelLarge: TextStyle get() = materialTypography.labelLarge
    val bodySmall: TextStyle get() = materialTypography.bodySmall
    val bodyMedium: TextStyle get() = materialTypography.bodyMedium
    val bodyLarge: TextStyle get() = materialTypography.bodyLarge

    companion object {
        fun getType(): AppTypography {
            return AppTypography(
                materialTypography = SharedTypography
            )
        }
    }
}
