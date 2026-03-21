package com.z_company.route.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.z_company.core.ui.theme.Shapes

private fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    color: Color,
    width: Dp = 4.dp,
    paddingEnd: Dp = 2.dp,
): Modifier = drawWithContent {
    drawContent()
    val viewportHeight = size.height
    val contentHeight = scrollState.maxValue + viewportHeight
    if (contentHeight > viewportHeight && scrollState.maxValue > 0) {
        val thumbHeight = (viewportHeight / contentHeight) * viewportHeight
        val thumbOffset = (scrollState.value.toFloat() / scrollState.maxValue) *
                (viewportHeight - thumbHeight)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - width.toPx() - paddingEnd.toPx(), thumbOffset),
            size = Size(width.toPx(), thumbHeight),
            cornerRadius = CornerRadius(width.toPx() / 2f)
        )
    }
}

@Composable
fun StationDropdownMenu(
    expanded: Boolean,
    stations: List<String>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState()
    val scrollbarColor = primaryColor.copy(alpha = 0.3f)

    if (stations.isNotEmpty() && expanded) {
        DropdownMenu(
            modifier = modifier
                .heightIn(max = 200.dp)
                .verticalScrollbar(scrollState, scrollbarColor),
            expanded = true,
            properties = PopupProperties(focusable = false),
            onDismissRequest = onDismiss,
            shape = Shapes.medium,
            containerColor = MaterialTheme.colorScheme.surface,
            scrollState = scrollState
        ) {
            stations.forEach { stationName ->
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stationName,
                                style = textStyle,
                                color = primaryColor,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clickable { onDelete(stationName) },
                                painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                                contentDescription = null,
                                tint = primaryColor
                            )
                        }
                    },
                    onClick = { onSelect(stationName) }
                )
            }
        }
    }
}
