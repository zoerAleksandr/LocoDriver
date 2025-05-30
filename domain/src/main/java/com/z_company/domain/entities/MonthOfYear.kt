package com.z_company.domain.entities

import java.util.Calendar
import java.util.UUID

enum class TagForDay {
    WORKING_DAY, NON_WORKING_DAY, SHORTENED_DAY, HOLIDAY,
}

data class MonthOfYear(
    var id: String = UUID.randomUUID().toString(),
    var year: Int = Calendar.getInstance().get(Calendar.YEAR),
    var month: Int = Calendar.getInstance().get(Calendar.MONTH),
    val days: List<Day> = listOf(),
    val tariffRate: Double = 0.0,
    val dateSetTariffRate: DateSetTariffRate? = null
)

data class Day(
    val dayOfMonth: Int,
    val tag: TagForDay,
    val isReleaseDay: Boolean = false
)

data class ReleasePeriod(
    val id: String = UUID.randomUUID().toString(),
    val days: List<Calendar> = listOf(),
)

data class DateSetTariffRate(
    val id: String = UUID.randomUUID().toString(),
    val dateNewRate: Int,
    val oldRate: Double,
//    val newRate: Double
)
