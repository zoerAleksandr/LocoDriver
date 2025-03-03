package com.z_company.core.ui.component.customDatePicker

import kotlinx.datetime.LocalDate

sealed class MySnappedDate(val snappedLocalDate: LocalDate, val snappedIndex: Int) {
    data class DayOfMonth(val localDate: LocalDate, val index: Int) :
        MySnappedDate(localDate, index)

    data class Month(val localDate: LocalDate, val index: Int) : MySnappedDate(localDate, index)
    data class Year(val localDate: LocalDate, val index: Int) : MySnappedDate(localDate, index)
}