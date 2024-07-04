package com.z_company.core.ui.theme

import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.z_company.core.ui.theme.custom.AppColors

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

@Composable
fun transparentColorForTextField() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)