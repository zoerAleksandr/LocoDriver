package com.z_company.domain.entities

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * year/month обязаны попадать в JSON даже у Json с encodeDefaults = false
 * (локальный settingsJson). Иначе месяц «переезжает» на текущий при чтении в
 * следующем месяце, а days остаются от старого — см. краш «31 сентября».
 */
class MonthOfYearSerializationTest {

    private val jsonWithoutDefaults = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun yearAndMonthAreAlwaysEncoded() {
        val now = MonthOfYear(days = listOf(Day(1, TagForDay.WORKING_DAY)))
        val encoded = jsonWithoutDefaults.encodeToString(MonthOfYear.serializer(), now)
        assertTrue(encoded.contains("\"year\""), "нет year: $encoded")
        assertTrue(encoded.contains("\"month\""), "нет month: $encoded")

        val decoded = jsonWithoutDefaults.decodeFromString(MonthOfYear.serializer(), encoded)
        assertTrue(decoded.year == now.year && decoded.month == now.month)
    }
}
