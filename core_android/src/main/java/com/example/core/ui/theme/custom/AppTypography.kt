package com.example.core.ui.theme.custom

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.core.R

@Immutable
data class AppTypography(
    val materialTypography: Typography
) {
    val titleLarge: TextStyle
        get() = materialTypography.titleLarge
    val titleMedium: TextStyle
        get() = materialTypography.titleMedium
    val titleSmall: TextStyle
        get() = materialTypography.titleSmall

    val displayLarge: TextStyle
        get() = materialTypography.displayLarge

    val displayMedium: TextStyle
        get() = materialTypography.displayLarge

    val displaySmall: TextStyle
        get() = materialTypography.displayLarge

    val headlineSmall: TextStyle
        get() = materialTypography.headlineSmall

    val headlineMedium: TextStyle
        get() = materialTypography.headlineMedium

    val headlineLarge: TextStyle
        get() = materialTypography.headlineLarge

    val labelSmall: TextStyle
        get() = materialTypography.labelSmall

    val labelMedium: TextStyle
        get() = materialTypography.labelMedium

    val labelLarge: TextStyle
        get() = materialTypography.labelLarge

    val bodySmall: TextStyle
        get() = materialTypography.bodySmall

    val bodyMedium: TextStyle
        get() = materialTypography.bodyMedium

    val bodyLarge: TextStyle
        get() = materialTypography.bodyLarge

    companion object {
        fun appTypography(): AppTypography {
            val fontFamily = AppFontFamilies.Oxygen
            val defaultTypography = Typography()
            return AppTypography(
                materialTypography = Typography(
                    displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
                    displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
                    displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),
                    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
                    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
                    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),
                    titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
                    titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
                    titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),
                    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
                    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
                    bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),
                    labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
                    labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
                    labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily)
                )
            )
        }

        @Immutable
        object AppFontFamilies {
            @Stable
            val Oxygen = FontFamily(
                Font(
                    resId = R.font.roboto_condensed_regular,
                    weight = FontWeight.Normal,
                    style = FontStyle.Normal
                ),
                Font(
                    resId = R.font.roboto_condensed_bold,
                    weight = FontWeight.Bold,
                    style = FontStyle.Normal
                ),
                Font(
                    resId = R.font.roboto_condensed_light,
                    weight = FontWeight.Light,
                    style = FontStyle.Normal
                ),
            )
        }
    }
}