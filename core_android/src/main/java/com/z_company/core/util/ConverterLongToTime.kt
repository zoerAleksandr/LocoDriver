package com.z_company.core.util

import java.text.SimpleDateFormat
import java.util.Locale

object ConverterLongToTime {
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

    fun getDateAndTimeStringFormat(long: Long?): String {
        return long?.let {
            SimpleDateFormat(
                "${DateAndTimeFormat.DATE_FORMAT} ${DateAndTimeFormat.TIME_FORMAT}",
                Locale.getDefault()
            ).format(it)
        } ?: ""
    }
}