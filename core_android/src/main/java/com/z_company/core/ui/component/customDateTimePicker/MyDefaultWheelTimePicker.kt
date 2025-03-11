package com.z_company.core.ui.component.customDateTimePicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.z_company.core.ui.component.customDatePicker.AmPm
import com.z_company.core.ui.component.customDatePicker.AmPmHour
import com.z_company.core.ui.component.customDatePicker.AmPmValue
import com.z_company.core.ui.component.customDatePicker.Hour
import com.z_company.core.ui.component.customDatePicker.MIN
import com.z_company.core.ui.component.customDatePicker.MAX
import com.z_company.core.ui.component.customDatePicker.Minute
import com.z_company.core.ui.component.customDatePicker.MySnappedTime
import com.z_company.core.ui.component.customDatePicker.MyWheelPickerDefaults
import com.z_company.core.ui.component.customDatePicker.MyWheelTextPicker
import com.z_company.core.ui.component.customDatePicker.SelectorProperties
import com.z_company.core.ui.component.customDatePicker.TimeFormat
import com.z_company.core.ui.component.customDatePicker.amPmHourToHour24
import com.z_company.core.ui.component.customDatePicker.amPmValueFromTime
import com.z_company.core.ui.component.customDatePicker.localTimeToAmPmHour
import com.z_company.core.ui.component.customDatePicker.now
import com.z_company.core.ui.component.customDatePicker.truncateTo
import com.z_company.core.ui.component.customDatePicker.withHour
import com.z_company.core.ui.component.customDatePicker.withMinute
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime

@Composable
internal fun MyDefaultWheelTimePicker(
    modifier: Modifier = Modifier,
    startTime: LocalTime = LocalTime.now(),
    minTime: LocalTime = LocalTime.MIN(),
    maxTime: LocalTime = LocalTime.MAX(),
    timeFormat: TimeFormat = TimeFormat.HOUR_24,
    height: Dp = 128.dp,
    rowCount: Int = 3,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
    textColor: Color = LocalContentColor.current,
    selectorProperties: SelectorProperties = MyWheelPickerDefaults.selectorProperties(),
    onSnappedTime: (snappedTime: MySnappedTime, timeFormat: TimeFormat) -> Int? = { _, _ -> null },
) {
    var snappedTime by remember { mutableStateOf(startTime.truncateTo(DateTimeUnit.MINUTE)) }

    val hours = (0..23).map {
        Hour(
            text = it.toString().padStart(2, '0'),
            value = it,
            index = it
        )
    }
    val amPmHours = (1..12).map {
        AmPmHour(
            text = it.toString(),
            value = it,
            index = it - 1
        )
    }

    val minutes = (0..59).map {
        Minute(
            text = it.toString().padStart(2, '0'),
            value = it,
            index = it
        )
    }

    val amPms = listOf(
        AmPm(
            text = "AM",
            value = AmPmValue.AM,
            index = 0
        ),
        AmPm(
            text = "PM",
            value = AmPmValue.PM,
            index = 1
        )
    )

    var snappedAmPm by remember {
        mutableStateOf(
            amPms.find { it.value == amPmValueFromTime(startTime) } ?: amPms[0]
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (selectorProperties.enabled().value) {
            Surface(
                modifier = Modifier.height(height / rowCount),
                color = selectorProperties.borderColor().value,
            ) {}
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            MyWheelTextPicker(
                modifier = Modifier.weight(1f),
                startIndex = if (timeFormat == TimeFormat.HOUR_24) {
                    hours.find { it.value == startTime.hour }?.index ?: 0
                } else amPmHours.find { it.value == localTimeToAmPmHour(startTime) }?.index
                    ?: 0,
                height = height,
                texts = if (timeFormat == TimeFormat.HOUR_24) hours.map { it.text } else amPmHours.map { it.text },
                rowCount = rowCount,
                style = textStyle,
                color = textColor
            ) { snappedIndex ->

                val newHour = if (timeFormat == TimeFormat.HOUR_24) {
                    hours.find { it.index == snappedIndex }?.value
                } else {
                    amPmHourToHour24(
                        amPmHours.find { it.index == snappedIndex }?.value ?: 0,
                        snappedTime.minute,
                        snappedAmPm.value
                    )
                }

                newHour?.let {

                    val newTime = snappedTime.withHour(newHour)

                    if (newTime.compareTo(minTime) >= 0 && newTime.compareTo(maxTime) <= 0) {
                        snappedTime = newTime
                    }

                    val newIndex = if (timeFormat == TimeFormat.HOUR_24) {
                        hours.find { it.value == snappedTime.hour }?.index
                    } else {
                        amPmHours.find { it.value == localTimeToAmPmHour(snappedTime) }?.index
                    }

                    newIndex?.let {
                        onSnappedTime(
                            MySnappedTime.Hour(
                                localTime = snappedTime,
                                index = newIndex
                            ),
                            timeFormat
                        )?.let { return@MyWheelTextPicker it }
                    }
                }

                return@MyWheelTextPicker if (timeFormat == TimeFormat.HOUR_24) {
                    hours.find { it.value == snappedTime.hour }?.index
                } else {
                    amPmHours.find { it.value == localTimeToAmPmHour(snappedTime) }?.index
                }
            }
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .height(height),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ":",
                    style = textStyle,
                    color = textColor
                )
            }
            MyWheelTextPicker(
                modifier = Modifier.weight(1f),
                startIndex = minutes.find { it.value == startTime.minute }?.index ?: 0,
                height = height,
                texts = minutes.map { it.text },
                rowCount = rowCount,
                style = textStyle,
                color = textColor
            ) { snappedIndex ->

                val newMinute = minutes.find { it.index == snappedIndex }?.value

                val newHour = if (timeFormat == TimeFormat.HOUR_24) {
                    hours.find { it.value == snappedTime.hour }?.value
                } else {
                    amPmHourToHour24(
                        amPmHours.find { it.value == localTimeToAmPmHour(snappedTime) }?.value
                            ?: 0,
                        snappedTime.minute,
                        snappedAmPm.value
                    )
                }

                newMinute?.let {
                    newHour?.let {
                        val newTime = snappedTime.withMinute(newMinute).withHour(newHour)

                        if (newTime.compareTo(minTime) >= 0 && newTime.compareTo(maxTime) <= 0) {
                            snappedTime = newTime
                        }

                        val newIndex =
                            minutes.find { it.value == snappedTime.minute }?.index

                        newIndex?.let {
                            onSnappedTime(
                                MySnappedTime.Minute(
                                    localTime = snappedTime,
                                    index = newIndex
                                ),
                                timeFormat
                            )?.let { return@MyWheelTextPicker it }
                        }
                    }
                }

                return@MyWheelTextPicker minutes.find { it.value == snappedTime.minute }?.index
            }
            if (timeFormat == TimeFormat.AM_PM) {
                MyWheelTextPicker(
                    modifier = Modifier.weight(1f),
                    startIndex = amPms.find { it.value == amPmValueFromTime(startTime) }?.index
                        ?: 0,
                    height = height,
                    texts = amPms.map { it.text },
                    rowCount = rowCount,
                    style = textStyle,
                    color = textColor
                ) { snappedIndex ->

                    val newAmPm = amPms.find {
                        if (snappedIndex == 2) {
                            it.index == 1
                        } else {
                            it.index == snappedIndex
                        }
                    }

                    newAmPm?.let {
                        snappedAmPm = newAmPm
                    }

                    val newMinute = minutes.find { it.value == snappedTime.minute }?.value

                    val newHour = amPmHourToHour24(
                        amPmHours.find { it.value == localTimeToAmPmHour(snappedTime) }?.value
                            ?: 0,
                        snappedTime.minute,
                        snappedAmPm.value
                    )

                    newMinute?.let {
                        val newTime = snappedTime.withMinute(newMinute).withHour(newHour)

                        if (newTime.compareTo(minTime) >= 0 && newTime.compareTo(maxTime) <= 0) {
                            snappedTime = newTime
                        }

                        val newIndex = minutes.find { it.value == snappedTime.hour }?.index

                        newIndex?.let {
                            onSnappedTime(
                                MySnappedTime.Hour(
                                    localTime = snappedTime,
                                    index = newIndex
                                ),
                                timeFormat
                            )
                        }
                    }

                    return@MyWheelTextPicker snappedIndex
                }
            }
        }
    }
}