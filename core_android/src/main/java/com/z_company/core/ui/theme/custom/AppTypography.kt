package com.z_company.core.ui.theme.custom

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.z_company.core.R


val SFFont = FontFamily(
    Font(R.font.rounded_light, FontWeight.Light),
    Font(R.font.rounded_regular, FontWeight.Normal),
    Font(R.font.rounded_medium, FontWeight.Medium),
    Font(R.font.rounded_semibold, FontWeight.SemiBold),
    Font(R.font.rounded_bold, FontWeight.Bold),
    Font(R.font.rounded_heavy, FontWeight.ExtraBold),
    Font(R.font.rounded_black, FontWeight.Black),
)

val Typography = Typography(
    // в figma обозначен title
    titleMedium = TextStyle(
        fontFamily = SFFont,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
    ),
    // в figma обозначен subtitle
    titleSmall = TextStyle(
        fontFamily = SFFont,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
    ),

    // в figma обозначен description
    bodyMedium = TextStyle(
        fontFamily = SFFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),

    // в figma обозначен data
    bodyLarge = TextStyle(
        fontFamily = SFFont,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
    ),

    // в figma обозначен button_text
    bodySmall = TextStyle(
        fontFamily = SFFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),

    // в figma обозначен bottom_menu
    labelMedium = TextStyle(
        fontFamily = SFFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
    ),
)

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
        get() = materialTypography.displayMedium

    val displaySmall: TextStyle
        get() = materialTypography.displaySmall

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
//        @Composable
        fun getType(): AppTypography {
//            val fontFamily = AppFontFamilies.RobotoConsed
            val defaultTypography = Typography
            return AppTypography(
                materialTypography = Typography(
                    displayLarge = defaultTypography.displayLarge.copy(),
                    displayMedium = defaultTypography.displayMedium.copy(),
                    displaySmall = defaultTypography.displaySmall.copy(),
                    headlineLarge = defaultTypography.headlineLarge.copy(),
                    headlineMedium = defaultTypography.headlineMedium.copy(),
                    headlineSmall = defaultTypography.headlineSmall.copy(),
                    titleLarge = defaultTypography.titleLarge.copy(),
                    titleMedium = defaultTypography.titleMedium.copy(),
                    titleSmall = defaultTypography.titleSmall.copy(),
                    bodyLarge = defaultTypography.bodyLarge.copy(),
                    bodyMedium = defaultTypography.bodyMedium.copy(),
                    bodySmall = defaultTypography.bodySmall.copy(),
                    labelLarge = defaultTypography.labelLarge.copy(),
                    labelMedium = defaultTypography.labelMedium.copy(),
                    labelSmall = defaultTypography.labelSmall.copy()
                )
            )
        }

//        @Immutable
//        object AppFontFamilies {
//            @Stable
//            val RobotoConsed = FontFamily(
//                Font(
//                    resId = R.font.montserrat_regular,
//                    weight = FontWeight.Normal,
//                    style = FontStyle.Normal
//                ),
//                Font(
//                    resId = R.font.montserrat_regular,
//                    weight = FontWeight.Bold,
//                    style = FontStyle.Normal
//                ),
//                Font(
//                    resId = R.font.montserrat_regular,
//                    weight = FontWeight.Light,
//                    style = FontStyle.Normal
//                ),
//            )
//        }
    }
}