package com.z_company.route.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes


@Composable
fun Chip(
    label: String,
    leading: @Composable (() -> Unit)? = null,
    selected: Boolean,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape = Shapes.medium,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 8.dp
) {
    val backgroundColor =
        if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.background
    val elevation = if (selected) 2.dp else 0.dp
    val border = if (selected) BorderStroke(
        width = 0.5.dp,
        color = MaterialTheme.colorScheme.secondary
    ) else null
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal

    Surface(
        onClick = onClick,
        shape = shape,
        color = backgroundColor,
        shadowElevation = elevation,
        border = border,
        modifier = Modifier
            .wrapContentWidth()
            .height(IntrinsicSize.Min)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            if (leading != null) {
                Box(modifier = Modifier.size(20.dp)) {
                    leading()
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = fontWeight
            )
        }
    }
}