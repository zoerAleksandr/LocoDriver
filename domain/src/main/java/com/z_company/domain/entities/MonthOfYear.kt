package com.z_company.domain.entities

import java.util.Calendar
import java.util.UUID

enum class TagForDay {
    WORKING_DAY, NON_WORKING_DAY, SHORTENED_DAY
}

data class MonthOfYear(
    var id: String = UUID.randomUUID().toString(),
    var year: Int = Calendar.getInstance().get(Calendar.YEAR),
    var month: Int = Calendar.getInstance().get(Calendar.MONTH),
    val days: List<Day> = listOf()
)

data class Day(
    val dayOfMonth: Int,
    val tag: TagForDay,
    val isReleaseDay: Boolean = false
)

data class ReleasePeriod(
    val id: String = UUID.randomUUID().toString(),
    val start: Calendar,
    val end: Calendar? = null
)
