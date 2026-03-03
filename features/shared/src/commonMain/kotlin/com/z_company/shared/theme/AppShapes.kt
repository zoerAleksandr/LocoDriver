package com.z_company.shared.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Stable

@Stable
data class AppShapes(
    val materialShapes: Shapes,
) {
    val small: CornerBasedShape get() = materialShapes.small
    val medium: CornerBasedShape get() = materialShapes.medium
    val large: CornerBasedShape get() = materialShapes.large

    companion object {
        fun appShapes(): AppShapes = AppShapes(com.z_company.shared.theme.Shapes)
    }
}
