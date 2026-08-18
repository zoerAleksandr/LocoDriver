@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.entities

import com.z_company.domain.util.generateId
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Личный день отвлечения пользователя (отпуск, больничный и т.д.).
 * Хранится отдельно от производственного календаря.
 * Синхронизируется с сервером как личные данные.
 *
 * Сохраняется списком — для одного периода создаётся несколько записей (по одной на день).
 */
@Serializable
data class ReleaseDay(
    val id: String = generateId(),
    val year: Int,
    val month: Int,        // 0-based (0=январь), как в MonthOfYear
    val dayOfMonth: Int,
    val releaseType: ReleaseType,
    // Явно заданные часы (только для ReleaseType.TechnicalStudy — «Технические
    // занятия»). Оплачиваются по среднему часу. Для остальных типов null.
    // Синхронизируется с сервером (новое поле контракта release_days).
    val hours: Double? = null,
) {
    fun toLocalDate(): LocalDate = LocalDate(year, month + 1, dayOfMonth)
}

/**
 * День производственного календаря (рабочий/выходной/сокращённый).
 * Загружается с сервера, не редактируется пользователем.
 * Общий для всех пользователей одной страны.
 */
@Serializable
data class ProductionCalendarDay(
    val country: String,   // "RU", "BY", "KZ" и т.д.
    val year: Int,
    val month: Int,        // 0-based
    val dayOfMonth: Int,
    val tag: TagForDay
)
