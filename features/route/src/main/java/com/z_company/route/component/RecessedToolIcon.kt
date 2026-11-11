package com.z_company.route.component

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.z_company.route.R
import com.z_company.loco_driver.ui.theme.LocalMashinistColors

/** Shared flat tool badge with an optional accent-tinted background. */
@Composable
fun RecessedToolIcon(
    @DrawableRes iconRes: Int? = null,
    modifier: Modifier = Modifier,
    diameter: Dp = 44.dp,
    iconSize: Dp = 36.dp,
    showBackground: Boolean = true,
    tint: Color = LocalMashinistColors.current.accent,
    glyph: (@Composable (Color) -> Unit)? = null,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val backgroundColor = tint.copy(alpha = if (dark) 0.15f else 0.10f)
    val context = LocalContext.current
    val mapUsesThemeColors = iconRes == R.drawable.ic_tool_map
    val drawableContext = if (mapUsesThemeColors) rememberThemeResourceContext(context, dark) else context

    Box(
        modifier = modifier
            .size(diameter)
            .then(
                if (showBackground) {
                    Modifier.background(backgroundColor, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (iconRes != null) {
            CompositionLocalProvider(LocalContext provides drawableContext) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    colorFilter = if (mapUsesThemeColors) null else ColorFilter.tint(tint),
                    modifier = Modifier.size(iconSize),
                )
            }
        } else if (glyph != null) {
            Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                glyph(tint)
            }
        }
    }
}

/** Resolves the map vector's @color resources against the active Compose theme,
 * including when the selected theme differs from Android's system night mode. */
@Composable
private fun rememberThemeResourceContext(context: Context, dark: Boolean): Context = remember(context, dark) {
    val configuration = Configuration(context.resources.configuration).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
    }
    context.createConfigurationContext(configuration)
}
