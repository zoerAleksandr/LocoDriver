@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.entities.norma_time

import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.util.generateId
import kotlin.time.Clock
import kotlinx.serialization.Serializable

/**
 * Станция справочника пользователя: нормы приёмки/сдачи + координаты и паспорт.
 * Координаты — единый источник для плеч обслуживания (связь по названию, см.
 * `StationDirectoryUseCase`). Запись есть только у станций с данными; «голые»
 * названия живут в `UserSettings.stationList` (выпадающий список).
 */
@Serializable
data class StationNorm(
    val stationId: String = generateId(),
    val name: String,
    val appearanceToStartMin: Int? = null,
    val endToBarrierMin: Int? = null,
    val barrierToStartMin: Int? = null,
    val endToWorkEndMin: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Код ЕСР (6 цифр). */
    val esr: String? = null,
    val osmId: Long? = null,
    /** null — тип неизвестен. */
    val kind: ServicePhaseStationKind? = null,
    /** Км оси станции по ж/д километражу. */
    val axisKm: Int? = null,
    /** Пикет оси, 1–10 (пк 1 — первые 100 м после километрового столба). */
    val axisPicket: Int? = null,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null

    val hasNorms: Boolean
        get() = appearanceToStartMin != null || endToBarrierMin != null ||
            barrierToStartMin != null || endToWorkEndMin != null

    /** Есть что хранить кроме названия — такую станцию нельзя удалять «из выпадающего списка». */
    val hasData: Boolean get() = hasNorms || hasCoordinates || esr != null || axisKm != null
}
