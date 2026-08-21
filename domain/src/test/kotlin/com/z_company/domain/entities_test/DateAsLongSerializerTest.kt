package com.z_company.domain.entities_test

import com.z_company.domain.entities.route.BasicData
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlinx.datetime.Instant

/**
 * Проверяет, как ТЕКУЩИЙ клиентский код (DateAsLongSerializer) разбирает
 * updatedAt именно в том формате, который СЕЙЧАС реально отдаёт сервер
 * (route.py:626, GET /v1/route/): "%b %d, %Y %H:%M:%S", UTC, без миллисекунд.
 *
 * Формат — легаси Gson-строка (Android-клиент до перехода на kotlinx-datetime).
 * Сервер её не меняет, поэтому клиент обязан уметь её парсить.
 */
@OptIn(ExperimentalTime::class)
class DateAsLongSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses real server date format correctly`() {
        // Ровно то, что отдаёт route.py: route.updated_at.strftime("%b %d, %Y %H:%M:%S")
        val serverJson = """{"updatedAt": "Aug 21, 2026 14:32:07"}"""
        val basicData = json.decodeFromString<BasicData>(serverJson)

        val expected = Instant.parse("2026-08-21T14:32:07Z").toEpochMilliseconds()
        assertEquals(expected, basicData.updatedAt)
    }

    @Test
    fun `parses server date format with single-digit day`() {
        // strftime %d всегда даёт 2 цифры (01..31), но проверяем и однозначный день
        // на случай ручных данных/миграций.
        val serverJson = """{"updatedAt": "Feb 5, 2026 09:04:00"}"""
        val basicData = json.decodeFromString<BasicData>(serverJson)

        val expected = Instant.parse("2026-02-05T09:04:00Z").toEpochMilliseconds()
        assertEquals(expected, basicData.updatedAt)
    }

    @Test
    fun `parses numeric epoch millis unchanged`() {
        // Формат, который вернёт сервер ПОСЛЕ перехода на epoch-ms (как уже сделано
        // для SalarySetting.updated_at на сервере).
        val millis = 1_787_000_000_000L
        val serverJson = """{"updatedAt": $millis}"""
        val basicData = json.decodeFromString<BasicData>(serverJson)

        assertEquals(millis, basicData.updatedAt)
    }

    @Test
    fun `unparseable value falls back to now instead of throwing`() {
        // Документирует существующее поведение: если формат неожиданный (например,
        // сервер вдруг отдаёт локализованное имя месяца), сериализатор не падает,
        // а тихо подставляет текущее время. Из-за этого "битая" дата ВСЕГДА выглядит
        // "новее" любой реальной локальной правки — источник хрупкости LWW-merge.
        val before = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val serverJson = """{"updatedAt": "не дата"}"""
        val basicData = json.decodeFromString<BasicData>(serverJson)
        val after = kotlin.time.Clock.System.now().toEpochMilliseconds()

        assert(basicData.updatedAt in before..after) {
            "Expected fallback to 'now' (between $before and $after), got ${basicData.updatedAt}"
        }
    }
}
