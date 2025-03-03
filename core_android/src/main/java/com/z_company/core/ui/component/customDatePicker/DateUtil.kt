package com.z_company.core.ui.component.customDatePicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import network.chaintech.kmp_date_time_picker.format


fun LocalDateTime.Companion.now(): LocalDateTime {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}

fun LocalDate.Companion.now(): LocalDate {
    return LocalDateTime.now().date
}

fun LocalTime.Companion.now(): LocalTime {
    return LocalDateTime.now().time
}

fun LocalTime.Companion.MIN(): LocalTime {
    return LocalTime(0, 0)
}

fun LocalTime.Companion.MAX(): LocalTime {
    return LocalTime(23, 59, 59, 999999999)
}

fun LocalDateTime.Companion.MIN() = LocalDateTime(
    year = 1900,
    monthNumber = 1,
    dayOfMonth = 1,
    hour = 0,
    minute = 0,
    second = 0,
    nanosecond = 0
)

fun LocalDateTime.Companion.MAX() = LocalDateTime(
    year = LocalDateTime.now().year + 10,
    monthNumber = 12,
    dayOfMonth = 31,
    hour = 0,
    minute = 0,
    second = 0,
    nanosecond = 0
)

fun LocalDate.Companion.MIN() = LocalDate(
    year = 1900,
    monthNumber = 1,
    dayOfMonth = 1,
)

fun LocalDate.Companion.MAX() = LocalDate(
    year = LocalDateTime.now().year + 100,
    monthNumber = 12,
    dayOfMonth = 31,
)

fun LocalDate.withDayOfMonth(dayOfMonth: Int) = LocalDate(this.year, this.month, dayOfMonth)

fun LocalDate.withMonth(month: Int): LocalDate {
    val length = month.monthLength(isLeapYear(this.year))
    return if (this.dayOfMonth > length) {
        LocalDate(this.year, month, length)
    } else {
        LocalDate(this.year, month, this.dayOfMonth)
    }
}

fun LocalDate.withYear(year: Int) = LocalDate(year, this.month, this.dayOfMonth)

fun LocalDateTime.withDayOfMonth(dayOfMonth: Int) =
    LocalDateTime(this.year, this.month, dayOfMonth, this.hour, this.minute, this.second)

fun LocalDateTime.withMonth(month: Int): LocalDateTime {
    val length = month.monthLength(isLeapYear(this.year))
    return if (this.dayOfMonth > length) {
        LocalDateTime(this.year, month, length, this.hour, this.minute, this.second)
    } else {
        LocalDateTime(this.year, month, this.dayOfMonth, this.hour, this.minute, this.second)
    }
}

fun LocalDateTime.withYear(year: Int) =
    LocalDateTime(year, this.month, this.dayOfMonth, this.hour, this.minute, this.second)

fun LocalDateTime.withHour(hour: Int) =
    LocalDateTime(this.year, this.month, this.dayOfMonth, hour, this.minute, this.second)

fun LocalDateTime.withMinute(minute: Int) =
    LocalDateTime(this.year, this.month, this.dayOfMonth, this.hour, minute, this.second)

fun LocalTime.withHour(hour: Int) =
    LocalTime(hour, this.minute, this.second)

fun LocalTime.withMinute(minute: Int) = LocalTime(this.hour, minute, this.second)

fun isLeapYear(year: Int): Boolean {
    val prolepticYear: Long = year.toLong()
    return prolepticYear and 3 == 0L && (prolepticYear % 100 != 0L || prolepticYear % 400 == 0L)
}

fun shortMonths(month: Int): String {
    return when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
}

fun String.capitalize(): String {
    return this.lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun LocalTime.truncateTo(unit: DateTimeUnit.TimeBased): LocalTime =
    LocalTime.fromNanosecondOfDay(toNanosecondOfDay().let { it - it % unit.nanoseconds })

fun LocalDateTime.truncateTo(unit: DateTimeUnit.TimeBased): LocalDateTime =
    LocalDateTime(date, time.truncateTo(unit))

fun shortDayOfWeek(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> ""
    }
}

const val ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
const val MONTH_YEAR = "MMMM YYYY"
const val SHORT_DAY = "EEE"

fun LocalDate.toMonthYear(): String {
    return this.asString(MONTH_YEAR).firstLetterUppercase()
}

fun LocalDate.toShortDay(): String {
    return this.asString(SHORT_DAY).uppercase()
}

fun LocalDate.asString(format: String = ISO8601): String {
    return format(this, format)
}

private fun String.firstLetterUppercase(): String {
    return this.replaceFirstChar { it.uppercase() }
}

fun Int.monthLength(isLeapYear: Boolean): Int {
    return when (this) {
        2 -> if (isLeapYear) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}

//@OptIn(ExperimentalStdlibApi::class)
//fun getFirstDayOfWeek(): LocalDate {
//    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
//    return today.daysShift(-DayOfWeek.entries.indexOf(today.dayOfWeek))
//}

private fun LocalDate.daysShift(days: Int): LocalDate = when {
    days < 0 -> {
        minus(1, DateTimeUnit.DayBased(-days))
    }

    days > 0 -> {
        plus(1, DateTimeUnit.DayBased(days))
    }

    else -> this
}

enum class Operation { PLUS, MINUS }

fun Modifier.noRippleEffect(
    onClick: () -> Unit
) = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

fun dateTimeToString(currentDateTime: LocalDateTime, givenFormat: String): String {
    return format(currentDateTime, outputFormat = givenFormat)
}

fun timeToString(currentTime: LocalTime, givenFormat: String): String {
    return format(currentTime, outputFormat = givenFormat)
}

enum class DateTimePickerView { BOTTOM_SHEET_VIEW, DIALOG_VIEW }

object WheelPickerDefaults {
    @Composable
    fun selectorProperties(
        enabled: Boolean = true,
        borderColor: Color = MaterialTheme.colorScheme.primary.copy(0.7f),
    ): SelectorProperties = DefaultSelectorProperties(
        enabled = enabled,
        borderColor = borderColor,
    )
}

interface SelectorProperties {
    @Composable
    fun enabled(): State<Boolean>

    @Composable
    fun borderColor(): State<Color>

}

@Immutable
internal class DefaultSelectorProperties(
    private val enabled: Boolean,
    private val borderColor: Color,
) : SelectorProperties {

    @Composable
    override fun enabled(): State<Boolean> {
        return rememberUpdatedState(enabled)
    }

    @Composable
    override fun borderColor(): State<Color> {
        return rememberUpdatedState(borderColor)
    }

}

fun calculateDayOfMonths(month: Int, year: Int): List<DayOfMonth> {
    val isLeapYear = isLeapYear(year)

    val month31day = (1..31).map {
        DayOfMonth(
            text = it.toString(),
            value = it,
            index = it - 1
        )
    }
    val month30day = (1..30).map {
        DayOfMonth(
            text = it.toString(),
            value = it,
            index = it - 1
        )
    }
    val month29day = (1..29).map {
        DayOfMonth(
            text = it.toString(),
            value = it,
            index = it - 1
        )
    }
    val month28day = (1..28).map {
        DayOfMonth(
            text = it.toString(),
            value = it,
            index = it - 1
        )
    }

    return when (month) {
        1 -> {
            month31day
        }

        2 -> {
            if (isLeapYear) month29day else month28day
        }

        3 -> {
            month31day
        }

        4 -> {
            month30day
        }

        5 -> {
            month31day
        }

        6 -> {
            month30day
        }

        7 -> {
            month31day
        }

        8 -> {
            month31day
        }

        9 -> {
            month30day
        }

        10 -> {
            month31day
        }

        11 -> {
            month30day
        }

        12 -> {
            month31day
        }

        else -> {
            emptyList()
        }
    }
}


data class DayOfMonth(
    val text: String,
    val value: Int,
    val index: Int
)

data class Month(
    val text: String,
    val value: Int,
    val index: Int
)

data class Year(
    val text: String,
    val value: Int,
    val index: Int
)

enum class TimeFormat {
    HOUR_24, AM_PM
}

data class Hour(
    val text: String,
    val value: Int,
    val index: Int
)

data class AmPmHour(
    val text: String,
    val value: Int,
    val index: Int
)

data class Minute(
    val text: String,
    val value: Int,
    val index: Int
)

data class AmPm(
    val text: String,
    val value: AmPmValue,
    val index: Int?
)

enum class AmPmValue {
    AM, PM
}

fun localTimeToAmPmHour(localTime: LocalTime): Int {
    if (
        isBetween(
            localTime,
            LocalTime(0, 0),
            LocalTime(0, 59)
        )
    ) {
        return localTime.hour + 12
    }

    if (
        isBetween(
            localTime,
            LocalTime(1, 0),
            LocalTime(11, 59)
        )
    ) {
        return localTime.hour
    }

    if (
        isBetween(
            localTime,
            LocalTime(12, 0),
            LocalTime(12, 59)
        )
    ) {
        return localTime.hour
    }

    if (
        isBetween(
            localTime,
            LocalTime(13, 0),
            LocalTime(23, 59)
        )
    ) {
        return localTime.hour - 12
    }

    return localTime.hour
}

fun isBetween(localTime: LocalTime, startTime: LocalTime, endTime: LocalTime): Boolean {
    return localTime in startTime..endTime
}

fun amPmHourToHour24(amPmHour: Int, amPmMinute: Int, amPmValue: AmPmValue): Int {
    return when (amPmValue) {
        AmPmValue.AM -> {
            if (amPmHour == 12 && amPmMinute <= 59) {
                0
            } else {
                amPmHour
            }
        }

        AmPmValue.PM -> {
            if (amPmHour == 12 && amPmMinute <= 59) {
                amPmHour
            } else {
                amPmHour + 12
            }
        }
    }
}

fun amPmValueFromTime(time: LocalTime): AmPmValue {
    return if (time.hour > 11) AmPmValue.PM else AmPmValue.AM
}