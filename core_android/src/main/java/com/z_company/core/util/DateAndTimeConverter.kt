package com.z_company.core.util

import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.getValue

object DateAndTimeConverter : KoinComponent {
    lateinit var timeZoneText: String
    private val settingsUseCase: SettingsUseCase by inject()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            settingsUseCase.getUserSettingFlow().collect { setting ->
                timeZoneText = settingsUseCase.getTimeZone(setting.timeZone)
            }
        }
    }

    fun getMonthShortText(value: Int?): String {
        return when (value) {
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

    fun getMonthFullText(value: Int?): String {
        return when (value) {
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
        return value?.let { millis ->
            val instant = Instant.ofEpochMilli(millis)
            val time = OffsetDateTime.ofInstant(instant, ZoneId.of(timeZoneText))
            val formatterWithThreeDecimals =
                DateTimeFormatter.ofPattern(DateAndTimeFormat.TIME_FORMAT)
            "${time.format(formatterWithThreeDecimals)}"
        } ?: ""
    }

    fun getDateMiniAndTime(value: Long?): String {
        if (value != null) {
            value.let { millis ->
                val instant = Instant.ofEpochMilli(millis)
                val time = OffsetDateTime.ofInstant(instant, ZoneId.of(timeZoneText))
                val formatterWithThreeDecimals =
                    DateTimeFormatter.ofPattern("dd.MM HH:mm")
                return ("${time.format(formatterWithThreeDecimals)}")
            }
        } else {
            return ("")
        }
    }

    fun getDateAndTime(value: Long): String {
        val instant = Instant.ofEpochMilli(value)
        val time = OffsetDateTime.ofInstant(instant, ZoneId.of(timeZoneText))
        val formatterWithThreeDecimals =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        return ("${time.format(formatterWithThreeDecimals)}")
    }

    fun getTimeFromDateLong(value: Long?): String {
        return value?.let { millis ->
            val instant = Instant.ofEpochMilli(value)
            val time = OffsetDateTime.ofInstant(instant, ZoneId.of(timeZoneText))
            val formatterWithThreeDecimals =
                DateTimeFormatter.ofPattern(DateAndTimeFormat.TIME_FORMAT)
            time.format(formatterWithThreeDecimals)
        } ?: ""
    }

    fun getDateFromDateLong(value: Long?): String {
        return value?.let { millis ->
            val instant = Instant.ofEpochMilli(value)
            val time = OffsetDateTime.ofInstant(instant, ZoneId.of(timeZoneText))
            val formatterWithThreeDecimals =
                DateTimeFormatter.ofPattern(DateAndTimeFormat.DATE_FORMAT)
            time.format(formatterWithThreeDecimals)
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