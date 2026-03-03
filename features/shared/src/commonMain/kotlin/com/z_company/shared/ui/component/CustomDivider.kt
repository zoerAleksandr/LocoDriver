package com.z_company.shared.ui.component

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DividerOrientation { Vertical, Horizontal }

@Composable
fun CustomDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    thickness: Dp = 1.dp,
    orientation: DividerOrientation
) {
    when (orientation) {
        DividerOrientation.Vertical -> {
            VerticalDivider(
                modifier = modifier.fillMaxHeight().width(thickness),
                color = color
            )
        }
        DividerOrientation.Horizontal -> {
            HorizontalDivider(
                modifier = modifier.fillMaxWidth().height(thickness),
                color = color
            )
        }
    }
}
