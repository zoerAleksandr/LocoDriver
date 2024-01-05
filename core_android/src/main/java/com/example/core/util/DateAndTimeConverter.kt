package com.example.core.util

import java.util.Calendar

object DateAndTimeConverter {
    private val currentTimeInLong = Calendar.getInstance().timeInMillis
    fun getDayOfMonth(date: Long?): String {
        val day = Calendar.getInstance().apply {
            timeInMillis = date ?: currentTimeInLong
        }.get(Calendar.DAY_OF_MONTH).toString()

        return if (day.length == 1) {
            "0$day"
        } else {
            day
        }
    }

    fun getMonthShorthand(date: Long?): String {
        return Calendar.getInstance().apply {
            timeInMillis = date ?: currentTimeInLong
        }.get(Calendar.MONTH).getMonthShortText()
    }

    fun Int.getMonthShortText(): String {
        return when (this) {
            0 -> {
                MonthShorthand.JANUARY
            }
            1 -> {
                MonthShorthand.FEBRUARY
            }
            2 -> {
                MonthShorthand.MARCH
            }
            3 -> {
                MonthShorthand.APRIL
            }
            4 -> {
                MonthShorthand.MAY
            }
            5 -> {
                MonthShorthand.JUNE
            }
            6 -> {
                MonthShorthand.JULY
            }
            7 -> {
                MonthShorthand.AUGUST
            }
            8 -> {
                MonthShorthand.SEPTEMBER
            }
            9 -> {
                MonthShorthand.OCTOBER
            }
            10 -> {
                MonthShorthand.NOVEMBER
            }
            11 -> {
                MonthShorthand.DECEMBER
            }
            else -> {
                ""
            }
        }
    }


    fun getHourInDate(date: Long): Int {
        val totalMinute = date / 60_000
        return (totalMinute / 60).toInt()
    }

    fun getRemainingMinuteFromHour(date: Long): Int {
        val totalMinute = date / 60_000
        return (totalMinute.rem(60)).toInt()
    }

    fun getTimeInStringFormat(date: Long?): String {
        return if (date == null) {
            "--/--"
        } else {
            val hour = getHourInDate(date)
            val hourText = if (hour < 10) {
                "0$hour"
            } else {
                hour.toString()
            }
            val minute = getRemainingMinuteFromHour(date)
            val minuteText = if (minute < 10) {
                "0$minute"
            } else {
                minute.toString()
            }
            "$hourText:$minuteText"
        }
    }

    fun Long?.compareWithNullable(other: Long?): Boolean {
        return if (this == null || other == null) true
        else this < other
    }
}