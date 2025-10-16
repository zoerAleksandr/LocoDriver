package com.z_company.loco_driver.ui.theme

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
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
    ),
)