package com.z_company.core.util

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Small font",
    group = "Font scale",
    fontScale = 0.8f
)
@Preview(
    name = "Normal font",
    group = "Font scale",
    fontScale = 1f
)
@Preview(
    name = "Large font",
    group = "Font scale",
    fontScale = 1.25f
)
annotation class FontScalePreviews

@Preview(
    name = "Dark mode",
    group = "UI mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    showBackground = true
)
@Preview(
    name = "Light mode",
    group = "UI mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    showBackground = true
)
annotation class DarkLightPreviews