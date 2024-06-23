package com.z_company.domain.entities

import java.util.Calendar

enum class TagForDay {
    WORKING_DAY, NON_WORKING_DAY, RELEASE_FROM_WORK, SHORTENED_DAY
}

data class MonthOfYear(
    var id: Int = 0,
    var year: Int = Calendar.getInstance().get(Calendar.YEAR),
    var month: Int = Calendar.getInstance().get(Calendar.MONTH),
    val days: List<Day> = listOf()
)

data class Day(
    val dayOfMonth: Int,
    val tag: TagForDay
)
