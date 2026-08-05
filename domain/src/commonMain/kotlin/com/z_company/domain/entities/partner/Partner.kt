@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.entities.partner

import com.z_company.domain.util.generateId
import kotlin.time.Clock
import kotlinx.serialization.Serializable

/**
 * Запись справочника напарников (Настройки → Справочники → Напарники).
 * У каждого пользователя свой список людей, с которыми он работает.
 *
 * Хранится и синхронизируется отдельно от маршрутов (full-replace эндпоинты
 * `/v1/partners/`, по образцу справочников норм времени
 * [com.z_company.domain.entities.norma_time.StationNorm]). В маршруте хранится
 * КОПИЯ выбранного напарника — см. [com.z_company.domain.entities.route.RoutePartner].
 *
 * [tabNumber] — табельный номер (строка: возможны ведущие нули/буквы).
 */
@Serializable
data class Partner(
    val partnerId: String = generateId(),
    val fullName: String = "",
    val tabNumber: String? = null,
    val notes: String? = null,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)
