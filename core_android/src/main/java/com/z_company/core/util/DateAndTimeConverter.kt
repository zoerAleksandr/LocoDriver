package com.z_company.core.util

import com.z_company.domain.entities.UserSettings
import com.z_company.domain.use_cases.SettingsUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.getValue

class DateAndTimeConverter(userSettings: UserSettings) : KoinComponent {
    var timeZoneText: String
    private val settingsUseCase: SettingsUseCase by inject()

    init {
            timeZoneText = settingsUseCase.getTimeZone(userSettings.timeZone)
    }

    fun getDate(value: Long?): String {
        if (value != null) {
            value.let { millis ->
                val instant = Instant.ofEpochMilli(millis)
                val time = OffsetDateTime.ofInstant(instant, ZoneId.of(timeZoneText))
                val formatterWithThreeDecimals =
                    DateTimeFormatter.ofPattern("dd.MM.yy")
                return ("${time.format(formatterWithThreeDecimals)}")
            }
        } else {
            return ("")
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