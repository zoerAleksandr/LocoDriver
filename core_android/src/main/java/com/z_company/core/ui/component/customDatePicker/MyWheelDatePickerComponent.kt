package com.z_company.core.ui.component.customDatePicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.custom.AppTypography
import kotlinx.datetime.LocalDate

object MyWheelDatePickerComponent {

    /***
     * modifier: Modifies the layout of the date picker.
     * title: Title displayed above the date picker.
     * doneLabel: Label for the "Done" button.
     * titleStyle: Style for the title text.
     * doneLabelStyle: Style for the "Done" label text.
     * startDate: Initial date selected in the picker.
     * minDate: Minimum selectable date.
     * maxDate: Maximum selectable date.
     * yearsRange: Initial years range.
     * height: height of the date picker component.
     * rowCount: Number of rows displayed in the picker and it's depending on height also.
     * showShortMonths: show short month name.
     * dateTextStyle: Text style for the date display.
     * dateTextColor: Text color for the date display.
     * hideHeader: Hide header of picker.
     * selectorProperties: Properties defining the interaction with the date picker.
     * onDoneClick: Callback triggered when the "Done" button is clicked, passing the selected date.
     * onDateChangeListener: Callback triggered when the Date is changed, passing the selected date.
     ***/

    @Composable
    fun MyWheelDatePicker(
        modifier: Modifier = Modifier,
        title: String = "Дата",
        doneLabel: String = "Выбрать",
        titleStyle: TextStyle = AppTypography.getType().titleLarge.copy(
            color = MaterialTheme.colorScheme.primary
        ),
        doneLabelStyle: TextStyle = AppTypography.getType().titleMedium
            .copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            ),
        startDate: LocalDate = LocalDate.now(),
        minDate: LocalDate = LocalDate.MIN(),
        maxDate: LocalDate = LocalDate.MAX(),
        yearsRange: IntRange? = IntRange(1922, 2122),
        height: Dp = 128.dp,
        rowCount: Int = 3,
        showShortMonths: Boolean = false,
        showMonthAsNumber: Boolean = false,
        dateTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
        dateTextColor: Color = LocalContentColor.current,
        hideHeader: Boolean = false,
        selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
        onDoneClick: (snappedDate: LocalDate) -> Unit = {},
        onDateChangeListener: (snappedDate: LocalDate) -> Unit = {},
    ) {
        var selectedDate by remember { mutableStateOf(LocalDate.now()) }

        LaunchedEffect(selectedDate) {
            if (hideHeader) {
                onDateChangeListener(selectedDate)
            }
        }

        Column(modifier = modifier) {
            if (!hideHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = titleStyle,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = doneLabel,
                        style = doneLabelStyle,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.noRippleEffect {
                            onDoneClick(selectedDate)
                        }
                    )
                }
            }

            MyDefaultWheelDatePicker(
                textColor = dateTextColor,
                selectorProperties = selectorProperties,
                rowCount = rowCount,
                height = height,
                modifier = Modifier.padding(top = 32.dp, bottom = 14.dp),
                startDate = startDate,
                minDate = minDate,
                maxDate = maxDate,
                yearsRange = yearsRange,
                showShortMonths = showShortMonths,
                showMonthAsNumber = showMonthAsNumber,
                textStyle = dateTextStyle,
                onSnappedDate = { snappedDate ->
                    selectedDate = snappedDate.snappedLocalDate
                    snappedDate.snappedIndex
                }
            )
        }
    }

}