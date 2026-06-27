package com.z_company.loco_driver.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.loco_driver.ui.theme.MashinistTheme

@Composable
fun GroupHeader(
    text: String,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MashinistTheme.colors.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(
            start = 4.dp,
            end = 4.dp,
            top = if (isFirst) 4.dp else 22.dp,
            bottom = 8.dp,
        ),
    )
}
