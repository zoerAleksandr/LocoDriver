package com.z_company.core.ui.component.customDateTimePicker


import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.core.ui.component.customDatePicker.MAX
import com.z_company.core.ui.component.customDatePicker.MIN
import com.z_company.core.ui.component.customDatePicker.MyWheelPickerDefaults
import com.z_company.core.ui.component.customDatePicker.SelectorProperties
import com.z_company.core.ui.component.customDatePicker.TimeFormat
import com.z_company.core.ui.component.customDatePicker.now
import kotlinx.datetime.LocalDateTime

@Composable
fun MyWheelDateTimePickerDialog(
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
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = RoundedCornerShape(10.dp),
    selectorProperties: SelectorProperties = MyWheelPickerDefaults.selectorProperties(),
    onDoneClick: (snappedDate: LocalDateTime) -> Unit = {},
    onDateChangeListener: (snappedDate: LocalDateTime) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if (showDatePicker) {
        Dialog(
            onDismissRequest = { onDismiss() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .noRippleEffect {
                        onDismiss()
                    }
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .wrapContentSize()
                        .animateContentSize(),
                    shape = shape,
                    color = containerColor,
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
                        showMonthAsNumber = showMonthAsNumber,
                        rowCount = rowCount,
                        dateTextStyle = dateTextStyle,
                        dateTextColor = dateTextColor,
                        hideHeader = hideHeader,
                        selectorProperties = selectorProperties,
                        onDoneClick = {
                            onDoneClick(it)
                        },
                        onDateChangeListener = onDateChangeListener
                    )
                }
            }
        }
    }
}

fun Modifier.noRippleEffect(
    onClick: () -> Unit
) = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}