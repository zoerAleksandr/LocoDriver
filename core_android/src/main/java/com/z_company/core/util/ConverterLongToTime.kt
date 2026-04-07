@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.core.util

import android.annotation.SuppressLint
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

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
            val hourText = if (hour < 10) {
                "0$hour"
            } else {
                hour.toString()
            }
            val minute = getRemainingMinuteFromHour(long)
            val minuteText = if (minute < 10) {
                "0$minute"
            } else {
                minute.toString()
            }
            "$hourText:$minuteText"
        }
    }

    @SuppressLint("DefaultLocale")
    fun getTimeInStringDecimalFormat(timeInMillis: Long?): String {
        return if (timeInMillis == null) {
            ""
        } else {
            val timeInMinutes = timeInMillis / 60000  // 60000 = 1000 мс * 60 сек

            val hours = timeInMinutes / 60

            val remainingMinutes = timeInMinutes % 60

            val decimalPart = remainingMinutes / 60.0

            val minute = "%.2f".format(decimalPart).drop(2)

            return String.format("%d,%s", hours, minute)
        }
    }

    fun formatDurationFromMillis(millis: Long?): String {
        if (millis == null) {
            return ""
        }
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

    @SuppressLint("DefaultLocale")
    fun getTimeInHourDecimal(long: Long?): String {
        return if (long == null) {
            "0,00"
        } else {
            val hour = long / 3_600_000.toDouble()
            String.format("%.2f", hour)
        }
    }


    fun timestampToDateTime(timestamp: Long): LocalDateTime {
        val instant: Instant = Instant.fromEpochMilliseconds(timestamp)
        return instant.toLocalDateTime(TimeZone.currentSystemDefault())
    }


}