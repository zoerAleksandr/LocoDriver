package com.z_company.route.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Chip(
    modifier: Modifier = Modifier,
    label: String,
    leading: @Composable (() -> Unit)? = null,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = Shapes.medium,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 8.dp,
    selectedBackgroundColor: Color = MaterialTheme.colorScheme.primary,
    unSelectedBackgroundColor: Color = MaterialTheme.colorScheme.background,
) {
    val backgroundColor =
        if (selected) selectedBackgroundColor else unSelectedBackgroundColor
    val border = if (selected) BorderStroke(
        width = 0.5.dp,
        color = MaterialTheme.colorScheme.secondary
    ) else BorderStroke(
        width = 0.5.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    )
    val textColor =
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = shape,
        color = backgroundColor,
        border = border,
        modifier = modifier
            .wrapContentWidth()
            .height(IntrinsicSize.Min)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberRipple(
                    color = MaterialTheme.colorScheme.background,
                    bounded = true
                ),
                onClick = onClick,
                onLongClick = onLongClick
            )
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
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = fontWeight
            )
        }
    }
}