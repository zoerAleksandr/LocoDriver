package com.z_company.core.ui.component.customDatePicker

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWheelDatePickerView(
    modifier: Modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
    showDatePicker: Boolean = false,
    title: String = "Дата",
    doneLabel: String = "Выбрать",
    startDate: LocalDate = LocalDate.now(),
    minDate: LocalDate = LocalDate.MIN(),
    maxDate: LocalDate = LocalDate.MAX(),
    yearsRange: IntRange? = IntRange(1922, 2122),
    height: Dp,
    rowCount: Int = 5,
    showShortMonths: Boolean = false,
    showMonthAsNumber: Boolean = false,
    dateTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
    dateTextColor: Color = LocalContentColor.current,
    hideHeader: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = RoundedCornerShape(10.dp),
    dateTimePickerView: DateTimePickerView = DateTimePickerView.DIALOG_VIEW,
    selectorProperties: SelectorProperties = MyWheelPickerDefaults.selectorProperties(),
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    onDoneClick: (snappedDate: LocalDate) -> Unit = {},
    onDateChangeListener: (snappedDate: LocalDate) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if (dateTimePickerView == DateTimePickerView.BOTTOM_SHEET_VIEW) {
        MyWheelDatePickerBottomSheet(
            modifier = modifier,
            showDatePicker = showDatePicker,
            title = title,
            doneLabel = doneLabel,
            minDate = minDate,
            maxDate = maxDate,
            yearsRange = yearsRange,
            height = height,
            rowCount = rowCount,
            showShortMonths = showShortMonths,
            dateTextStyle = dateTextStyle,
            dateTextColor = dateTextColor,
            hideHeader = hideHeader,
            containerColor = containerColor,
            showMonthAsNumber=showMonthAsNumber,
            shape = shape,
            selectorProperties = selectorProperties,
            dragHandle = dragHandle,
            onDoneClick = onDoneClick,
            onDateChangeListener = onDateChangeListener,
            onDismiss = onDismiss
        )
    } else {
        MyWheelDatePickerDialog(
            modifier = modifier,
            showDatePicker = showDatePicker,
            title = title,
            doneLabel = doneLabel,
            startDate = startDate,
            minDate = minDate,
            maxDate = maxDate,
            yearsRange = yearsRange,
            height = height,
            rowCount = rowCount,
            showShortMonths = showShortMonths,
            dateTextStyle = dateTextStyle,
            dateTextColor = dateTextColor,
            showMonthAsNumber=showMonthAsNumber,
            hideHeader = hideHeader,
            containerColor = containerColor,
            shape = shape,
            selectorProperties = selectorProperties,
            onDoneClick = onDoneClick,
            onDateChangeListener = onDateChangeListener,
            onDismiss = onDismiss
        )
    }
}