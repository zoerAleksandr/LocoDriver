package com.z_company.core.ui.component.customDatePicker


import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.LocalDate

@Composable
fun MyWheelDatePickerDialog(
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
    shape: Shape = RoundedCornerShape(10.dp),
    selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
    onDoneClick: (snappedDate: LocalDate) -> Unit = {},
    onDateChangeListener: (snappedDate: LocalDate) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if (showDatePicker) {
        Dialog(
            onDismissRequest = { onDismiss() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .noRippleEffect { onDismiss() }
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .wrapContentSize()
                        .animateContentSize(),
                    shape = shape,
                    color = containerColor,
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
                        showMonthAsNumber = showMonthAsNumber,
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
    }
}