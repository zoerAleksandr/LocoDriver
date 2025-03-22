package com.z_company.core.ui.component.customDateTimePicker


import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import kotlinx.datetime.LocalDateTime
import com.z_company.core.ui.component.customDatePicker.MAX
import com.z_company.core.ui.component.customDatePicker.MIN
import com.z_company.core.ui.component.customDatePicker.MyWheelPickerDefaults
import com.z_company.core.ui.component.customDatePicker.SelectorProperties
import com.z_company.core.ui.component.customDatePicker.TimeFormat
import com.z_company.core.ui.component.customDatePicker.now

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWheelDateTimePickerBottomSheet(
    modifier: Modifier = Modifier,
    showDatePicker: Boolean = false,
    title: String = "Дата и время",
    doneLabel: String = "Выбрать",
    timeFormat: TimeFormat = TimeFormat.HOUR_24,
    startDate: LocalDateTime = LocalDateTime.now(),
    minDate: LocalDateTime = LocalDateTime.MIN(),
    maxDate: LocalDateTime = LocalDateTime.MAX(),
    yearsRange: IntRange? = IntRange(1922, 2122),
    height: Dp,
    rowCount: Int = 3,
    dateTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
    dateTextColor: Color = LocalContentColor.current,
    hideHeader: Boolean = false,
    showMonthAsNumber: Boolean = false,
    containerColor: Color = Color.White,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    selectorProperties: SelectorProperties = MyWheelPickerDefaults.selectorProperties(),
    onDoneClick: (snappedDate: LocalDateTime) -> Unit = {},
    onDateChangeListener: (snappedDate: LocalDateTime) -> Unit = {},
    onDismiss: () -> Unit = {},
    onSettingClick: () -> Unit,
) {
    if (showDatePicker) {
        val modalBottomSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = modalBottomSheetState,
            dragHandle = dragHandle,
            shape = shape,
            containerColor = containerColor,
        ) {
            MyWheelDateTimePickerComponent.MyWheelDateTimePicker(
                modifier = modifier,
                title = title,
                timeFormat = timeFormat,
                doneLabel = doneLabel,
                startDateTime = startDate,
                minDateTime = minDate,
                maxDateTime = maxDate,
                yearsRange = yearsRange,
                height = height,
                rowCount = rowCount,
                showMonthAsNumber = showMonthAsNumber,
                dateTextStyle = dateTextStyle,
                dateTextColor = dateTextColor,
                hideHeader = hideHeader,
                selectorProperties = selectorProperties,
                onDoneClick = {
                    onDoneClick(it)
                },
                onDateChangeListener = onDateChangeListener,
                onSettingClick = onSettingClick
            )
        }
    }
}