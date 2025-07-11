package com.z_company.core.ui.component.customDatePicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import com.z_company.core.ui.theme.Shapes
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
import com.z_company.core.ui.component.toDp
import com.z_company.core.util.DateAndTimeConverter
//import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
//import com.z_company.core.util.DateAndTimeConverter.getMonthShortText
import kotlinx.datetime.LocalDate

@Composable
fun MyDefaultWheelDatePicker(
    modifier: Modifier = Modifier,
    startDate: LocalDate = LocalDate.now(),
    minDate: LocalDate = LocalDate.MIN(),
    maxDate: LocalDate = LocalDate.MAX(),
    yearsRange: IntRange? = IntRange(1922, 2122),
    height: Dp = 128.dp,
    rowCount: Int = 3,
    showShortMonths: Boolean = false,
    showMonthAsNumber: Boolean = false, // Added flag to show month as a number
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    textColor: Color = LocalContentColor.current,
    selectorProperties: SelectorProperties = MyWheelPickerDefaults.selectorProperties(),
    onSnappedDate: (snappedDate: MySnappedDate) -> Int? = { _ -> null }
) {
//    val dateAndTimeConverter = DateAndTimeConverter()
    var snappedDate by remember { mutableStateOf(startDate) }

    var dayOfMonths = calculateDayOfMonths(snappedDate.monthNumber, snappedDate.year)

    val months = (0..11).map {
        Month(
            text = when {
                showMonthAsNumber -> it.toString() // Show month as number
                showShortMonths -> DateAndTimeConverter.getMonthShortText(it) // Show short month name
                else -> DateAndTimeConverter.getMonthFullText(it).capitalize() // Show full month name
            },
            value = it,
            index = it - 1
        )
    }

    val years = yearsRange?.map {
        Year(
            text = it.toString(),
            value = it,
            index = yearsRange.indexOf(it)
        )
    }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            years?.let { years ->
                MyWheelTextPicker(
                    modifier = Modifier.weight(1f),
                    startIndex = years.find { it.value == startDate.year }?.index ?: 0,
                    height = height,
                    texts = years.map { it.text },
                    rowCount = rowCount,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    contentAlignment = Alignment.CenterEnd,
                ) { snappedIndex ->

                    val newYear = years.find { it.index == snappedIndex }?.value

                    newYear?.let {

                        val newDate = snappedDate.withYear(newYear)

                        if (newDate.compareTo(minDate) >= 0 && newDate.compareTo(maxDate) <= 0) {
                            snappedDate = newDate
                        }

                        dayOfMonths =
                            calculateDayOfMonths(snappedDate.monthNumber, snappedDate.year)

                        val newIndex = years.find { it.value == snappedDate.year }?.index

                        newIndex?.let {
                            onSnappedDate(
                                MySnappedDate.Year(
                                    localDate = snappedDate,
                                    index = newIndex
                                )
                            )?.let { return@MyWheelTextPicker it }

                        }
                    }

                    return@MyWheelTextPicker years.find { it.value == snappedDate.year }?.index
                }
            }

            MyWheelTextPicker(
                modifier = Modifier.weight(1f),
                startIndex = months.find { it.value == startDate.monthNumber }?.index ?: 0,
                height = height,
                texts = months.map { it.text },
                rowCount = rowCount,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                contentAlignment = Alignment.Center,
            ) { snappedIndex ->

                val newMonth = months.find { it.index == snappedIndex }?.value

                newMonth?.let {
                    val newDate = snappedDate.withMonth(newMonth)

                    if (newDate.compareTo(minDate) >= 0 && newDate.compareTo(maxDate) <= 0) {
                        snappedDate = newDate
                    }

                    dayOfMonths =
                        calculateDayOfMonths(snappedDate.monthNumber, snappedDate.year)

                    val newIndex = months.find { it.value == snappedDate.monthNumber }?.index

                    newIndex?.let {
                        onSnappedDate(
                            MySnappedDate.Month(
                                localDate = snappedDate,
                                index = newIndex
                            )
                        )?.let { return@MyWheelTextPicker it }
                    }
                }

                return@MyWheelTextPicker months.find { it.value == snappedDate.monthNumber }?.index
            }

            MyWheelTextPicker(
                modifier = Modifier.weight(1f),
                startIndex = dayOfMonths.find { it.value == startDate.dayOfMonth }?.index ?: 0,
                height = height,
                texts = dayOfMonths.map { it.text },
                rowCount = rowCount,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                contentAlignment = Alignment.CenterStart,
            ) { snappedIndex ->

                val newDayOfMonth = dayOfMonths.find { it.index == snappedIndex }?.value

                newDayOfMonth?.let {
                    val newDate = snappedDate.withDayOfMonth(newDayOfMonth)

                    if (newDate.compareTo(minDate) >= 0 && newDate.compareTo(maxDate) <= 0) {
                        snappedDate = newDate
                    }

                    val newIndex =
                        dayOfMonths.find { it.value == snappedDate.dayOfMonth }?.index

                    newIndex?.let {
                        onSnappedDate(
                            MySnappedDate.DayOfMonth(
                                localDate = snappedDate,
                                index = newIndex
                            )
                        )?.let { return@MyWheelTextPicker it }
                    }
                }

                return@MyWheelTextPicker dayOfMonths.find { it.value == snappedDate.dayOfMonth }?.index
            }

        }
    }
}