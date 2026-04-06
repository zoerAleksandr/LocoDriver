package com.z_company.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TimeManager(val timeZoneId: String = "GMT+3") {

    /** Текущий момент, обрезанный до минуты (UTC epoch). */
    fun now(): Long {
        val ms = System.currentTimeMillis()
        return ms - (ms % 60_000L)
    }

    /** Форматирует epoch в "HH:mm" в заданном часовом поясе. */
    fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault())
            .also { it.timeZone = TimeZone.getTimeZone(timeZoneId) }
            .format(Date(millis))

    /** Форматирует epoch в "dd.MM.yy" в заданном часовом поясе. */
    fun formatDate(millis: Long): String =
        SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            .also { it.timeZone = TimeZone.getTimeZone(timeZoneId) }
            .format(Date(millis))
}
