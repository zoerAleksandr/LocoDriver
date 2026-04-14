package com.z_company.core.util

import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.util.displayTimeZone
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class DateAndTimeConverter(userSettings: UserSettings) {
    // Timezone определяется через единый центр displayTimeZone():
    // Россия/Беларусь → GMT+3 (московское), Казахстан → GMT+5 (местное KZT)
    val timeZoneText: String = userSettings.displayTimeZone()

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

    fun getDateAndTime(value: Long?): String {
        if (value != null) {
            if (value == 0L){
                return ("Нет данных")
            }
            value.let { millis ->
                val instant = Instant.ofEpochMilli(millis)
                val time = OffsetDateTime.ofInstant(instant, ZoneId.of(timeZoneText))
                val formatterWithThreeDecimals =
                    DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")
                return ("${time.format(formatterWithThreeDecimals)}")
            }
        } else {
            return ("")
        }
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

    /**
     * Преобразует локальную дату и время в epoch-миллисекунды с учётом timezone отображения.
     * Используется при сохранении явки в разделе График.
     *
     * @param year      год
     * @param month     месяц (0-based, как в Calendar / MonthOfYear.month)
     * @param day       день месяца (1-based)
     * @param hour      часы (0..23)
     * @param minute    минуты (0..59)
     */
    fun toEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val zone = ZoneId.of(timeZoneText)
        val local = LocalDateTime.of(year, month + 1, day, hour, minute, 0)
        return local.atZone(zone).toInstant().toEpochMilli()
    }

    /** Возвращает день месяца из epoch-миллисекунд в timezone отображения. */
    fun getDayOfMonth(millis: Long): Int =
        Instant.ofEpochMilli(millis).atZone(ZoneId.of(timeZoneText)).dayOfMonth

    /** Возвращает месяц (0-based) из epoch-миллисекунд в timezone отображения. */
    fun getMonthIndex(millis: Long): Int =
        Instant.ofEpochMilli(millis).atZone(ZoneId.of(timeZoneText)).monthValue - 1

    fun isDifferenceDate(first: Long?, second: Long?): Boolean {
        return if (first != null && second != null) {
            val tz = ZoneId.of(timeZoneText)
            val firstDate = Instant.ofEpochMilli(first).atZone(tz).toLocalDate()
            val secondDate = Instant.ofEpochMilli(second).atZone(tz).toLocalDate()
            firstDate != secondDate
        } else {
            false
        }
    }
}
