package com.z_company.shared.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

/**
 * KMP-compatible extension to convert a [TextUnit] (sp) to [Dp].
 * Equivalent to core_android `com.z_company.core.ui.component.toDp`.
 */
@Composable
fun TextUnit.toDp(): Dp {
    val density = LocalDensity.current
    return if (this.isSpecified) {
        with(density) { this@toDp.toDp() }
    } else {
        Dp.Unspecified
    }
}
