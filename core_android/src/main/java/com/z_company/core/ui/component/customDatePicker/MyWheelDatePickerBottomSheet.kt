package com.z_company.core.ui.component.customDatePicker


import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWheelDatePickerBottomSheet(
    modifier: Modifier = Modifier,
    showDatePicker: Boolean = false,
    title: String = "Due Date",
    doneLabel: String = "Done",
    titleStyle: TextStyle = LocalTextStyle.current,
    doneLabelStyle: TextStyle = LocalTextStyle.current,
    startDate: LocalDate = LocalDate.now(),
    minDate: LocalDate = LocalDate.MIN(),
    maxDate: LocalDate = LocalDate.MAX(),
    yearsRange: IntRange? = IntRange(1922, 2122),
    height: Dp,
    rowCount: Int = 3,
    showShortMonths: Boolean = false,
    showMonthAsNumber: Boolean = false,
    dateTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
    dateTextColor: Color = LocalContentColor.current,
    hideHeader: Boolean = false,
    containerColor: Color = Color.White,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
    onDoneClick: (snappedDate: LocalDate) -> Unit = {},
    onDateChangeListener: (snappedDate: LocalDate) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if (showDatePicker) {
        val modalBottomSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = modalBottomSheetState,
            containerColor = containerColor,
            dragHandle = dragHandle,
            shape = shape
        ) {
            MyWheelDatePickerComponent.MyWheelDatePicker(
                modifier = modifier,
                title = title,
                doneLabel = doneLabel,
                titleStyle = titleStyle,
                doneLabelStyle = doneLabelStyle,
                startDate = startDate,
                minDate = minDate,
                maxDate = maxDate,
                yearsRange = yearsRange,
                height = height,
                rowCount = rowCount,
                showShortMonths = showShortMonths,
                showMonthAsNumber=showMonthAsNumber,
                dateTextStyle = dateTextStyle,
                dateTextColor = dateTextColor,
                hideHeader = hideHeader,
                selectorProperties = selectorProperties,
                onDoneClick = {
                    onDoneClick(it)
                },
                onDateChangeListener = onDateChangeListener,
            )
        }
    }
}