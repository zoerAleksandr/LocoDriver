package com.z_company.shared.util

/**
 * KMP-compatible replacement for core_android MonthShortenedText.
 * Hardcoded Russian short month names (same as the Android string resources).
 */
object MonthShortenedText {
    fun getMonthShortText(value: Int?): String {
        return when (value) {
            0 -> "ЯНВ"
            1 -> "ФЕВ"
            2 -> "МАР"
            3 -> "АПР"
            4 -> "МАЙ"
            5 -> "ИЮН"
            6 -> "ИЮЛ"
            7 -> "АВГ"
            8 -> "СЕН"
            9 -> "ОКТ"
            10 -> "НОЯ"
            11 -> "ДЕК"
            else -> ""
        }
    }
}
