package com.z_company.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * `ModalBottomSheet` приложения без нажимаемой ручки. Штатный контейнер маркера перетаскивания у Material 3 кликабелен
 * (ripple/серый прямоугольник и подсказка «Drag handle» по тапу), поэтому настоящая ручка отключается (`dragHandle = null`),
 * а [dragHandle] (по умолчанию [AppDragHandle]) рисуется первой строкой содержимого. Сама шторка по-прежнему тянется жестом.
 * `dragHandle = null` - без маркера.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    sheetGesturesEnabled: Boolean = true,
    dragHandle: (@Composable () -> Unit)? = { AppDragHandle() },
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        sheetGesturesEnabled = sheetGesturesEnabled,
        dragHandle = null,
    ) {
        if (dragHandle != null) Box(Modifier.align(Alignment.CenterHorizontally)) { dragHandle() }
        content()
    }
}
