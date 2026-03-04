package com.z_company.shared.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Shared time formatting utilities for both Android and iOS.
 */
object TimeFormatter {
    val monthNames = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    /** Format epoch millis to "dd.MM.yyyy HH:mm" */
    fun formatDateTime(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val d = ldt.dayOfMonth.toString().padStart(2, '0')
        val mo = ldt.monthNumber.toString().padStart(2, '0')
        val h = ldt.hour.toString().padStart(2, '0')
        val min = ldt.minute.toString().padStart(2, '0')
        return "$d.$mo.${ldt.year} $h:$min"
    }

    /** Format epoch millis to "dd.MM.yyyy" (date only) */
    fun formatDate(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val d = ldt.dayOfMonth.toString().padStart(2, '0')
        val mo = ldt.monthNumber.toString().padStart(2, '0')
        return "$d.$mo.${ldt.year}"
    }

    /** Format epoch millis to "HH:mm" (time only) */
    fun formatTime(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val h = ldt.hour.toString().padStart(2, '0')
        val min = ldt.minute.toString().padStart(2, '0')
        return "$h:$min"
    }

    /** Format epoch millis to "dd.MM HH:mm" (short) */
    fun formatDateTimeShort(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val d = ldt.dayOfMonth.toString().padStart(2, '0')
        val mo = ldt.monthNumber.toString().padStart(2, '0')
        val h = ldt.hour.toString().padStart(2, '0')
        val min = ldt.minute.toString().padStart(2, '0')
        return "$d.$mo $h:$min"
    }

    /** Format duration millis to "Xч Yмин" */
    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "—"
        val hours = millis / 3_600_000
        val minutes = (millis % 3_600_000) / 60_000
        return if (hours > 0) "${hours}ч ${minutes}мин" else "${minutes}мин"
    }

    /** Format duration millis to "HH:MM" */
    fun formatDurationHHMM(millis: Long): String {
        if (millis <= 0) return "00:00"
        val hours = millis / 3_600_000
        val minutes = (millis % 3_600_000) / 60_000
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }

    /** Formats milliseconds as "X ч" or "X ч Y мин" */
    fun formatHoursFromMs(ms: Long): String {
        val hours = ms / 3_600_000L
        val minutes = (ms % 3_600_000L) / 60_000L
        return if (minutes > 0) "$hours ч $minutes мин" else "$hours ч"
    }

    /** Formats timezone offset to readable string */
    fun formatTimeZone(timeZoneMs: Long): String {
        val totalHoursFromMoscow = timeZoneMs / 3_600_000L
        val moscowOffset = 3L + totalHoursFromMoscow
        return if (totalHoursFromMoscow == 0L) {
            "МСК (UTC+3)"
        } else {
            "UTC+$moscowOffset (МСК${if (totalHoursFromMoscow > 0) "+" else ""}$totalHoursFromMoscow)"
        }
    }
}
