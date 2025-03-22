package com.z_company.core.ui.component.customDatePicker

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

sealed class MySnappedDate(val snappedLocalDate: LocalDate, val snappedIndex: Int) {
    data class DayOfMonth(val localDate: LocalDate, val index: Int) :
        MySnappedDate(localDate, index)

    data class Month(val localDate: LocalDate, val index: Int) : MySnappedDate(localDate, index)
    data class Year(val localDate: LocalDate, val index: Int) : MySnappedDate(localDate, index)
}

sealed class MySnappedDateTime(
    val snappedLocalDateTime: LocalDateTime,
    val snappedIndex: Int
) {
    data class DayOfMonth(val localDateTime: LocalDateTime, val index: Int) :
        MySnappedDateTime(localDateTime, index)

    data class Month(val localDateTime: LocalDateTime, val index: Int) :
        MySnappedDateTime(localDateTime, index)

    data class Year(val localDateTime: LocalDateTime, val index: Int) :
        MySnappedDateTime(localDateTime, index)

    data class Hour(val localDateTime: LocalDateTime, val index: Int) :
        MySnappedDateTime(localDateTime, index)

    data class Minute(val localDateTime: LocalDateTime, val index: Int) :
        MySnappedDateTime(localDateTime, index)
}

internal sealed class MySnappedTime(val snappedLocalTime: LocalTime) {
    data class Hour(val localTime: LocalTime, val index: Int) : MySnappedTime(localTime)
    data class Minute(val localTime: LocalTime, val index: Int) : MySnappedTime(localTime)
}