@file:Suppress("DEPRECATION")
package com.z_company.shared.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * KMP-compatible replacement for core_android ConverterLongToTime.
 *
 * Utility functions for converting time durations (in milliseconds) to formatted strings.
 */
object ConverterLongToTime {

    fun getHourInDate(date: Long): Int {
        val totalMinute = date / 60_000
        return (totalMinute / 60).toInt()
    }

    fun getHour(long: Long): Int {
        val totalMinute = long / 60_000
        return (totalMinute / 60).toInt()
    }

    fun getRemainingMinuteFromHour(long: Long): Int {
        val totalMinute = long / 60_000
        return (totalMinute.rem(60)).toInt()
    }

    fun getTimeInStringFormat(long: Long?): String {
        return if (long == null) {
            "          "
        } else if (long < 0) {
            "00:00"
        } else {
            val hour = getHour(long)
            val hourText = if (hour < 10) "0$hour" else hour.toString()
            val minute = getRemainingMinuteFromHour(long)
            val minuteText = if (minute < 10) "0$minute" else minute.toString()
            "$hourText:$minuteText"
        }
    }

    fun getTimeInStringDecimalFormat(timeInMillis: Long?): String {
        if (timeInMillis == null) return ""
        val timeInMinutes = timeInMillis / 60000
        val hours = timeInMinutes / 60
        val remainingMinutes = timeInMinutes % 60
        val decimalPart = remainingMinutes / 60.0
        val rounded = kotlin.math.round(decimalPart * 100).toInt()
        val minute = if (rounded < 10) "0$rounded" else rounded.toString()
        return "$hours,$minute"
    }

    fun formatDurationFromMillis(millis: Long?): String {
        if (millis == null) return ""
        val totalMinutes = millis / 60000
        val days = totalMinutes / 1440
        val hours = (totalMinutes % 1440) / 60
        val minutes = totalMinutes % 60

        return buildString {
            if (days > 0) append("${days}д ")
            if (hours > 0) append("${hours}ч ")
            if (minutes >= 0 || isEmpty()) append("${minutes}м")
        }.trim()
    }

    fun getTimeInHourDecimal(long: Long?): String {
        if (long == null) return "0,00"
        val hour = long / 3_600_000.toDouble()
        val rounded = kotlin.math.round(hour * 100) / 100
        val intPart = rounded.toLong()
        val fracPart = kotlin.math.round((rounded - intPart) * 100).toInt()
        val fracStr = if (fracPart < 10) "0$fracPart" else fracPart.toString()
        return "$intPart,$fracStr"
    }

    fun timestampToDateTime(timestamp: Long): LocalDateTime {
        val instant: Instant = Instant.fromEpochMilliseconds(timestamp)
        return instant.toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
