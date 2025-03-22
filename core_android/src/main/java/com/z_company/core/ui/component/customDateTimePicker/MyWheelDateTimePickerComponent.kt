package com.z_company.core.ui.component.customDateTimePicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.customDatePicker.MAX
import com.z_company.core.ui.component.customDatePicker.MIN
import com.z_company.core.ui.component.customDatePicker.MyWheelPickerDefaults
import com.z_company.core.ui.component.customDatePicker.SelectorProperties
import com.z_company.core.ui.component.customDatePicker.TimeFormat
import com.z_company.core.ui.component.customDatePicker.now
import com.z_company.core.ui.theme.custom.AppTypography
import kotlinx.datetime.LocalDateTime

object MyWheelDateTimePickerComponent {

    /***
     * modifier: Modifies the layout of the datetime picker.
     * title: Title displayed above the datetime picker.
     * doneLabel: Label for the "Done" button.
     * titleStyle: Style for the title text.
     * doneLabelStyle: Style for the "Done" label text.
     * minDateTime: Minimum selectable datetime.
     * maxDateTime: Maximum selectable datetime.
     * yearsRange: Initial years range.
     * timeFormat: Format for displaying time (e.g., 24-hour format).
     * height: height of the datetime picker component.
     * rowCount: Number of rows displayed in the picker and it's depending on height also.
     * dateTextStyle: Text style for the datetime display.
     * dateTextColor: Text color for the datetime display.
     * hideHeader: Hide header of picker.
     * selectorProperties: Properties defining the interaction with the datetime picker.
     * onDoneClick: Callback triggered when the "Done" button is clicked, passing the selected datetime.
     * onDateChangeListener: Callback triggered when the Date is changed, passing the selected datetime.
     ***/

    @Composable
    fun MyWheelDateTimePicker(
        modifier: Modifier = Modifier,
        title: String = "Дата и время",
        doneLabel: String = "Выбрать",
        startDateTime: LocalDateTime = LocalDateTime.now(),
        minDateTime: LocalDateTime = LocalDateTime.MIN(),
        maxDateTime: LocalDateTime = LocalDateTime.MAX(),
        yearsRange: IntRange? = IntRange(1922, 2122),
        timeFormat: TimeFormat = TimeFormat.HOUR_24,
        height: Dp = 128.dp,
        rowCount: Int = 3,
        dateTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
        dateTextColor: Color = LocalContentColor.current,
        hideHeader: Boolean = false,
        showMonthAsNumber: Boolean = false,
        selectorProperties: SelectorProperties = MyWheelPickerDefaults.selectorProperties(),
        onDoneClick: (snappedDate: LocalDateTime) -> Unit = {},
        onDateChangeListener: (snappedDate: LocalDateTime) -> Unit = {},
        onSettingClick: () -> Unit
    ) {
        val titleStyle: TextStyle = AppTypography.getType().titleLarge.copy(
            color = MaterialTheme.colorScheme.primary
        )

        val doneLabelStyle: TextStyle = AppTypography.getType().titleMedium
            .copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )

        var selectedDate by remember { mutableStateOf(LocalDateTime.now()) }

        LaunchedEffect(selectedDate) {
            if (hideHeader) {
                onDateChangeListener(selectedDate)
            }
        }

        Column(modifier) {
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
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = doneLabel,
                        style = doneLabelStyle,
                        modifier = Modifier
                            .noRippleEffect {
                                onDoneClick(selectedDate)
                            }
                    )
                }
            }
            MyDefaultWheelDateTimePicker(
                textColor = dateTextColor,
                timeFormat = timeFormat,
                selectorProperties = selectorProperties,
                rowCount = rowCount,
                height = height,
                modifier = Modifier.padding(top = 32.dp, bottom = 14.dp),
                startDateTime = startDateTime,
                minDateTime = minDateTime,
                maxDateTime = maxDateTime,
                yearsRange = yearsRange,
                showMonthAsNumber = showMonthAsNumber,
                textStyle = dateTextStyle,
                onSnappedDateTime = { snappedDateTime ->
                    selectedDate = snappedDateTime.snappedLocalDateTime
                    snappedDateTime.snappedIndex
                }
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .noRippleEffect { onSettingClick() },
                    text = "Изменить интерфейс",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

}
