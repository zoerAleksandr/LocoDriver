package com.z_company.iosapp.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object TimeFormatter {
    // month names list
    val monthNames = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    // Format epoch millis to "dd.MM.yyyy HH:mm"
    fun formatDateTime(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val d = ldt.dayOfMonth.toString().padStart(2, '0')
        val mo = ldt.monthNumber.toString().padStart(2, '0')
        val h = ldt.hour.toString().padStart(2, '0')
        val min = ldt.minute.toString().padStart(2, '0')
        return "$d.$mo.${ldt.year} $h:$min"
    }

    // Format epoch millis to "dd.MM HH:mm" (short)
    fun formatDateTimeShort(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val d = ldt.dayOfMonth.toString().padStart(2, '0')
        val mo = ldt.monthNumber.toString().padStart(2, '0')
        val h = ldt.hour.toString().padStart(2, '0')
        val min = ldt.minute.toString().padStart(2, '0')
        return "$d.$mo $h:$min"
    }

    // Format duration millis to "Xч Yмин"
    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "\u2014"
        val hours = millis / 3_600_000
        val minutes = (millis % 3_600_000) / 60_000
        return if (hours > 0) "${hours}ч ${minutes}мин" else "${minutes}мин"
    }

    // Format duration millis to "HH:MM"
    fun formatDurationHHMM(millis: Long): String {
        if (millis <= 0) return "00:00"
        val hours = millis / 3_600_000
        val minutes = (millis % 3_600_000) / 60_000
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }
}
