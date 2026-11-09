package com.z_company.route.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.z_company.route.component.servicephase.Pst
import com.z_company.route.component.servicephase.PstIcon
import com.z_company.route.component.servicephase.PstIcons

private val HeaderTileHeight = 44.dp

/** Лёгкая кнопка-плитка для шапки обычного экрана. */
@Composable
fun HeaderTile(
    onClick: () -> Unit,
    description: String,
    size: Dp = HeaderTileHeight,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(Pst.surface, shape)
            .border(BorderStroke(1.dp, Pst.border), shape)
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun HeaderBackTile(onClick: () -> Unit, modifier: Modifier = Modifier, description: String = "Назад") {
    HeaderTile(onClick = onClick, description = description, modifier = modifier) {
        PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text)
    }
}

@Composable
fun HeaderCloseTile(onClick: () -> Unit, modifier: Modifier = Modifier, description: String = "Закрыть") {
    HeaderTile(onClick = onClick, description = description, modifier = modifier) {
        PstIcon(PstIcons.Close, 20.dp, Pst.text)
    }
}

@Composable
fun HeaderTextTile(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Pst.text,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(HeaderTileHeight)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(Pst.surface, shape)
            .border(BorderStroke(1.dp, Pst.border), shape)
            .clickable(enabled = enabled, onClickLabel = text, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    ScreenHeader(
        modifier = modifier,
        navigation = navigation,
        actions = actions,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Pst.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        },
    )
}

@Composable
fun ScreenHeader(
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    title: @Composable () -> Unit,
) {
    Layout(
        content = {
            Box(contentAlignment = Alignment.CenterStart) { navigation() }
            Box(contentAlignment = Alignment.Center) { title() }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { actions() }
        },
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val navigationPlaceable = measurables[0].measure(loose)
        val actionsPlaceable = measurables[2].measure(loose)
        val gap = 8.dp.roundToPx()
        val side = maxOf(navigationPlaceable.width, actionsPlaceable.width)
        val titleMax = (constraints.maxWidth - 2 * (side + gap)).coerceAtLeast(0)
        val titlePlaceable = measurables[1].measure(loose.copy(maxWidth = titleMax))
        val width = constraints.maxWidth
        val height = maxOf(
            HeaderTileHeight.roundToPx(),
            navigationPlaceable.height,
            actionsPlaceable.height,
            titlePlaceable.height,
        )
        layout(width, height) {
            navigationPlaceable.place(0, (height - navigationPlaceable.height) / 2)
            actionsPlaceable.place(width - actionsPlaceable.width, (height - actionsPlaceable.height) / 2)
            titlePlaceable.place((width - titlePlaceable.width) / 2, (height - titlePlaceable.height) / 2)
        }
    }
}
