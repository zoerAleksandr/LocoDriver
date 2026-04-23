package com.z_company.domain.entities.calendar

import kotlinx.serialization.Serializable

/**
 * Регион (субъект РФ или другой страны), для которого могут быть свои
 * региональные праздники поверх стандартного производственного календаря.
 *
 * @property code ISO 3166-2 код, например "RU-TA" (Татарстан), "RU-BA" (Башкортостан).
 * @property displayName Название региона на русском для отображения в UI.
 * @property country Код страны (RU/BY/KZ) — для фильтрации по стране в UI.
 */
@Serializable
data class Region(
    val code: String,
    val displayName: String,
    val country: String,
)

/**
 * Региональный праздничный день — нерабочий день дополнительно к
 * федеральному календарю. При наложении на стандартный календарь имеет
 * приоритет: день получает тег HOLIDAY независимо от стандартного тега.
 */
@Serializable
data class RegionalHoliday(
    val region: String,
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val name: String,
)
