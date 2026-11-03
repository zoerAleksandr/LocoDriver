package com.z_company.repository.remote_rest.tracks

import kotlinx.serialization.Serializable

/*
 * Контракт `/v1/gnss/zones` (сервер: docs/GNSS_DEAD_ZONES_PLAN.md §12). Все поля camelCase; лишние поля игнорируются.
 * Нет user_id, времени (кроме UTC-дня поездки) и точных координат. Координаты, ячейки и tag не логируются.
 */

/** Ячейка вклада: `kinds` - маска GAP=1, POOR=2, JUMP=4; `count` 1..100 (информативно). */
@Serializable
data class GnssContribCellDto(val iy: Int, val ix: Int, val kinds: Int, val count: Int = 1)

/** `POST /v1/gnss/zones/contrib`. `tag` - случайный UUID устройства (ротация 30 дней), `step` - версия сетки. */
@Serializable
data class GnssZoneContribRequest(
    val contribId: String,
    val tag: String,
    val step: Int = 1,
    val day: String? = null,
    val cells: List<GnssContribCellDto>,
)

@Serializable
data class GnssZoneContribResponse(val accepted: Boolean = false, val duplicate: Boolean = false)

/** Ячейка общей карты: `level` 1..3, `kinds` - маска. */
@Serializable
data class GnssZoneCellDto(val iy: Int, val ix: Int, val level: Int, val kinds: Int = 0)

/** `GET /v1/gnss/zones`. */
@Serializable
data class GnssZonesResponse(
    val step: Int = 1,
    val updatedAt: Long = 0,
    val cells: List<GnssZoneCellDto> = emptyList(),
)
