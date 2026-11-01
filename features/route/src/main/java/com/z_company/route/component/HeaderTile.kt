package com.z_company.route.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.z_company.route.component.servicephase.Pst

/**
 * Лёгкая кнопка-плитка для шапки обычного экрана (не поверх карты): без тени, с тонкой рамкой.
 * Поверх карты остаётся `MapButton` с тенью - там подложка нужна для контраста.
 */
@Composable
fun HeaderTile(onClick: () -> Unit, description: String, size: Dp = 44.dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Pst.surface, shape)
            .border(BorderStroke(1.dp, Pst.border), shape)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
