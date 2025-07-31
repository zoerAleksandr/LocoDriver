package com.z_company.core.util

import android.annotation.SuppressLint
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Locale

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