package com.z_company.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Маркер перетаскивания шторок (bottom sheet) приложения. В отличие от `BottomSheetDefaults.DragHandle` не кликабелен:
 * нет ripple и индикации нажатия. Размеры и цвет как у стандартного маркера Material 3 (32x4 dp, onSurfaceVariant 40 %).
 * Использовать во всех `ModalBottomSheet`: `dragHandle = { AppDragHandle() }`.
 */
@Composable
fun AppDragHandle(
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    height: Dp = 4.dp,
) {
    Surface(
        modifier = modifier.padding(vertical = 22.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(50),
    ) {
        Box(Modifier.size(width = width, height = height))
    }
}
