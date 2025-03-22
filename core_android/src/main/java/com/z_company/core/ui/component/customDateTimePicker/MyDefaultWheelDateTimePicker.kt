package com.z_company.core.ui.component.customDateTimePicker


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.customDatePicker.MAX
import com.z_company.core.ui.component.customDatePicker.MIN
import com.z_company.core.ui.component.customDatePicker.MyDefaultWheelDatePicker
import com.z_company.core.ui.component.customDatePicker.MySnappedDate
import com.z_company.core.ui.component.customDatePicker.MySnappedDateTime
import com.z_company.core.ui.component.customDatePicker.MySnappedTime
import com.z_company.core.ui.component.customDatePicker.MyWheelPickerDefaults
import com.z_company.core.ui.component.customDatePicker.SelectorProperties
import com.z_company.core.ui.component.customDatePicker.TimeFormat
import com.z_company.core.ui.component.customDatePicker.localTimeToAmPmHour
import com.z_company.core.ui.component.customDatePicker.now
import com.z_company.core.ui.component.customDatePicker.truncateTo
import com.z_company.core.ui.component.customDatePicker.withDayOfMonth
import com.z_company.core.ui.component.customDatePicker.withHour
import com.z_company.core.ui.component.customDatePicker.withMinute
import com.z_company.core.ui.component.customDatePicker.withMonth
import com.z_company.core.ui.component.customDatePicker.withYear
import com.z_company.core.ui.component.toDp
import com.z_company.core.ui.theme.Shapes
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime

@Composable
internal fun MyDefaultWheelDateTimePicker(
    modifier: Modifier = Modifier,
    startDateTime: LocalDateTime = LocalDateTime.now(),
    minDateTime: LocalDateTime = LocalDateTime.MIN(),
    maxDateTime: LocalDateTime = LocalDateTime.MAX(),
    yearsRange: IntRange? = IntRange(1922, 2122),
    timeFormat: TimeFormat = TimeFormat.HOUR_24,
    height: Dp = 128.dp,
    rowCount: Int = 3,
    showMonthAsNumber: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
    textColor: Color = LocalContentColor.current,
    selectorProperties: SelectorProperties = MyWheelPickerDefaults.selectorProperties(),
    onSnappedDateTime: (snappedDateTime: MySnappedDateTime) -> Int? = { _ -> null }
) {
    var snappedDateTime by remember { mutableStateOf(startDateTime.truncateTo(DateTimeUnit.MINUTE)) }

    val yearTexts = yearsRange?.map { it.toString() } ?: listOf()

    val heightSelectLine = textStyle.fontSize.toDp() + 12.dp

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (selectorProperties.enabled().value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        shape = Shapes.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    .height(heightSelectLine)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MyDefaultWheelDatePicker(
                modifier = Modifier.weight(0.7f).padding(start = 8.dp),
                startDate = startDateTime.date,
                yearsRange = yearsRange,
                height = height,
                rowCount = rowCount,
                textStyle = textStyle,
                showMonthAsNumber = showMonthAsNumber,
                selectorProperties = MyWheelPickerDefaults.selectorProperties(enabled = false),
                showShortMonths = true,
                textColor = textColor
            ) { snappedDate ->

                val newDateTime = when (snappedDate) {
                    is MySnappedDate.DayOfMonth -> {
                        snappedDateTime.withDayOfMonth(snappedDate.snappedLocalDate.dayOfMonth)
                    }

                    is MySnappedDate.Month -> {
                        snappedDateTime.withMonth(snappedDate.snappedLocalDate.monthNumber)
                    }

                    is MySnappedDate.Year -> {
                        snappedDateTime.withYear(snappedDate.snappedLocalDate.year)
                    }

                }
                if (newDateTime.compareTo(minDateTime) >= 0 && newDateTime.compareTo(
                        maxDateTime
                    ) <= 0
                ) {
                    snappedDateTime = newDateTime
                }

                return@MyDefaultWheelDatePicker when (snappedDate) {
                    is MySnappedDate.DayOfMonth -> {
                        onSnappedDateTime(
                            MySnappedDateTime.DayOfMonth(
                                snappedDateTime,
                                snappedDateTime.dayOfMonth - 1
                            )
                        )
                        snappedDateTime.dayOfMonth - 1
                    }

                    is MySnappedDate.Month -> {
                        onSnappedDateTime(
                            MySnappedDateTime.Month(
                                snappedDateTime,
                                snappedDateTime.monthNumber - 1
                            )
                        )
                        snappedDateTime.monthNumber - 1
                    }

                    is MySnappedDate.Year -> {
                        onSnappedDateTime(
                            MySnappedDateTime.Year(
                                snappedDateTime,
                                yearTexts.indexOf(snappedDateTime.year.toString())
                            )
                        )
                        yearTexts.indexOf(snappedDateTime.year.toString())
                    }

                }
            }
            MyDefaultWheelTimePicker(
                modifier = Modifier.weight(0.3f).padding(end = 8.dp),
                startTime = startDateTime.time,
                timeFormat = timeFormat,
                height = height,
                rowCount = rowCount,
                textStyle = textStyle,
                textColor = textColor,
                selectorProperties = MyWheelPickerDefaults.selectorProperties(
                    enabled = false
                ),
                onSnappedTime = { snappedTime, timeFormat ->

                    val newDateTime = when (snappedTime) {
                        is MySnappedTime.Hour -> {
                            snappedDateTime.withHour(snappedTime.snappedLocalTime.hour)
                        }

                        is MySnappedTime.Minute -> {
                            snappedDateTime.withMinute(snappedTime.snappedLocalTime.minute)
                        }
                    }

                    if (newDateTime.compareTo(minDateTime) >= 0 && newDateTime.compareTo(
                            maxDateTime
                        ) <= 0
                    ) {
                        snappedDateTime = newDateTime
                    }

                    return@MyDefaultWheelTimePicker when (snappedTime) {
                        is MySnappedTime.Hour -> {
                            onSnappedDateTime(
                                MySnappedDateTime.Hour(
                                    snappedDateTime,
                                    snappedDateTime.hour
                                )
                            )
                            if (timeFormat == TimeFormat.HOUR_24) snappedDateTime.hour else
                                localTimeToAmPmHour(snappedDateTime.time) - 1
                        }

                        is MySnappedTime.Minute -> {
                            onSnappedDateTime(
                                MySnappedDateTime.Minute(
                                    snappedDateTime,
                                    snappedDateTime.minute
                                )
                            )
                            snappedDateTime.minute
                        }
                    }
                },
            )
        }
    }
}