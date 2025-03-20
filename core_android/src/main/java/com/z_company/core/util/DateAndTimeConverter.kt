package com.z_company.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
                MonthShortenedText.JANUARY
            }

            1 -> {
                MonthShortenedText.FEBRUARY
            }

            2 -> {
                MonthShortenedText.MARCH
            }

            3 -> {
                MonthShortenedText.APRIL
            }

            4 -> {
                MonthShortenedText.MAY
            }

            5 -> {
                MonthShortenedText.JUNE
            }

            6 -> {
                MonthShortenedText.JULY
            }

            7 -> {
                MonthShortenedText.AUGUST
            }

            8 -> {
                MonthShortenedText.SEPTEMBER
            }

            9 -> {
                MonthShortenedText.OCTOBER
            }

            10 -> {
                MonthShortenedText.NOVEMBER
            }

            11 -> {
                MonthShortenedText.DECEMBER
            }

            else -> {
                ""
            }
        }
    }

    fun Int.getMonthFullText(): String {
        return when (this) {
            0 -> {
                MonthFullText.JANUARY
            }

            1 -> {
                MonthFullText.FEBRUARY
            }

            2 -> {
                MonthFullText.MARCH
            }

            3 -> {
                MonthFullText.APRIL
            }

            4 -> {
                MonthFullText.MAY
            }

            5 -> {
                MonthFullText.JUNE
            }

            6 -> {
                MonthFullText.JULY
            }

            7 -> {
                MonthFullText.AUGUST
            }

            8 -> {
                MonthFullText.SEPTEMBER
            }

            9 -> {
                MonthFullText.OCTOBER
            }

            10 -> {
                MonthFullText.NOVEMBER
            }

            11 -> {
                MonthFullText.DECEMBER
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
            ""
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

    fun getTime(value: Long?): String {
        return if (value != null) {
            SimpleDateFormat(
                DateAndTimeFormat.TIME_FORMAT, Locale.getDefault()
            ).format(
                value
            )
        } else {
            ""
        }
    }

    fun getDateMiniAndTime(value: Long?): String {
        return if (value != null) {
            val time = value.let { millis ->
                SimpleDateFormat(
                    DateAndTimeFormat.TIME_FORMAT,
                    Locale.getDefault()
                ).format(
                    millis
                )
            }
            val date = value.let { millis ->
                SimpleDateFormat(
                    DateAndTimeFormat.MINI_DATE_FORMAT, Locale.getDefault()
                ).format(
                    millis
                )
            }
            "$date $time"
        } else {
            ""
        }
    }

    fun getDateAndTime(value: Long): String {
        val time = value.let { millis ->
            SimpleDateFormat(
                DateAndTimeFormat.TIME_FORMAT,
                Locale.getDefault()
            ).format(
                millis
            )
        }
        val date = value.let { millis ->
            SimpleDateFormat(
                DateAndTimeFormat.DATE_FORMAT, Locale.getDefault()
            ).format(
                millis
            )
        }
        return "$date $time"
    }

    fun getTimeFromDateLong(value: Long?): String {
        return value?.let { millis ->
            SimpleDateFormat(
                DateAndTimeFormat.TIME_FORMAT,
                Locale.getDefault()
            ).format(
                millis
            )
        } ?: ""
    }

    fun getDateFromDateLong(value: Long?): String {
        return value?.let { millis ->
            SimpleDateFormat(
                DateAndTimeFormat.DATE_FORMAT, Locale.getDefault()
            ).format(
                millis
            )
        } ?: ""
    }

    fun isDifferenceDate(first: Long?, second: Long?): Boolean {
        return if (first != null && second != null) {
            val firstDate = SimpleDateFormat(
                DateAndTimeFormat.DATE_FORMAT_ONLY_DAY_OF_MONTH, Locale.getDefault()
            ).format(
                first
            )

            val secondDate = SimpleDateFormat(
                DateAndTimeFormat.DATE_FORMAT_ONLY_DAY_OF_MONTH, Locale.getDefault()
            ).format(
                second
            )
            return firstDate != secondDate
        } else {
            false
        }
    }
}
