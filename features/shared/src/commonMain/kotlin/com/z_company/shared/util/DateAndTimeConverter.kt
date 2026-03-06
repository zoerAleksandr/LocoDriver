@file:Suppress("DEPRECATION")
package com.z_company.shared.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * KMP-compatible replacement for core_android DateAndTimeConverter.
 *
 * Converts epoch millis to formatted date/time strings using a given time zone ID.
 */
class DateAndTimeConverter(timeZoneId: String) {

    /** Convenience constructor from UserSettings (uses domain getTimeZone). */
    constructor(userSettings: com.z_company.domain.entities.setting.UserSettings) : this(
        com.z_company.domain.util.getTimeZone(userSettings.timeZone)
    )

    /** Timezone text for display (e.g. "GMT+3"). */
    val timeZoneText: String = timeZoneId

    private val timeZone: TimeZone = try {
        TimeZone.of(timeZoneId)
    } catch (_: Exception) {
        TimeZone.currentSystemDefault()
    }

    fun getDate(value: Long?): String {
        if (value == null) return ""
        val dt = Instant.fromEpochMilliseconds(value).toLocalDateTime(timeZone)
        val day = dt.dayOfMonth.pad2()
        val month = dt.monthNumber.pad2()
        val year = (dt.year % 100).pad2()
        return "$day.$month.$year"
    }

    fun getTime(value: Long?): String {
        if (value == null) return ""
        val dt = Instant.fromEpochMilliseconds(value).toLocalDateTime(timeZone)
        return "${dt.hour.pad2()}:${dt.minute.pad2()}"
    }

    fun getDateMiniAndTime(value: Long?): String {
        if (value == null) return ""
        val dt = Instant.fromEpochMilliseconds(value).toLocalDateTime(timeZone)
        val day = dt.dayOfMonth.pad2()
        val month = dt.monthNumber.pad2()
        return "$day.$month ${dt.hour.pad2()}:${dt.minute.pad2()}"
    }

    fun getDateAndTime(value: Long?): String {
        if (value == null) return ""
        if (value == 0L) return "Нет данных"
        val dt = Instant.fromEpochMilliseconds(value).toLocalDateTime(timeZone)
        val day = dt.dayOfMonth.pad2()
        val month = dt.monthNumber.pad2()
        val year = (dt.year % 100).pad2()
        return "$day.$month.$year ${dt.hour.pad2()}:${dt.minute.pad2()}"
    }

    fun getTimeFromDateLong(value: Long?): String {
        return getTime(value)
    }

    fun getDateFromDateLong(value: Long?): String {
        return getDate(value)
    }

    fun isDifferenceDate(first: Long?, second: Long?): Boolean {
        if (first == null || second == null) return false
        val dt1 = Instant.fromEpochMilliseconds(first).toLocalDateTime(timeZone)
        val dt2 = Instant.fromEpochMilliseconds(second).toLocalDateTime(timeZone)
        return dt1.dayOfMonth != dt2.dayOfMonth ||
                dt1.monthNumber != dt2.monthNumber ||
                dt1.year != dt2.year
    }

    private fun Int.pad2(): String = if (this < 10) "0$this" else this.toString()
}
