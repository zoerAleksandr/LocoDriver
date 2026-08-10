package com.z_company.route.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Раскладка пары элементов (обычно двух полей ввода «Принял → Сдал» и т.п.).
 *
 * При стандартном системном шрифте (fontScale ≤ [threshold]) — прежний **ряд**
 * из двух элементов по [Modifier.weight] с опциональным разделителем [separator]
 * между ними (стрелка «→»).
 *
 * При крупном шрифте два узких поля в ряд не помещаются и длинное значение
 * обрезается — тогда раскладываем элементы **друг под другом**, каждый на всю
 * ширину, чтобы значение было видно целиком.
 *
 * Важно: при стандартном шрифте вид не меняется — ветка [threshold] гарантирует,
 * что перекладка включается только выше «стандарта».
 */
@Composable
fun AdaptivePair(
    modifier: Modifier = Modifier,
    threshold: Float = 1.15f,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    rowVerticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    separator: (@Composable () -> Unit)? = null,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    val stacked = LocalDensity.current.fontScale > threshold
    if (stacked) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalAlignment = rowVerticalAlignment
        ) {
            first(Modifier.weight(1f))
            separator?.invoke()
            second(Modifier.weight(1f))
        }
    }
}
