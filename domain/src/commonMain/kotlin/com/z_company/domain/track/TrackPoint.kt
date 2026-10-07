package com.z_company.domain.track

/** Точка локального трека (SCREEN_SPECS §6.9). Координаты не логировать. */
data class TrackPoint(
    /** Время фикса, мс от Unix epoch (UTC). */
    val timeMs: Long,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double?,
    val accuracyM: Float?,
    /** Высота, м; пока не пишется (отдельное согласие №2), столбец зарезервирован. */
    val altitudeM: Double? = null,
)

/** Заголовок трека: один непрерывный сеанс «АВТО» на одном поезде. */
data class TrackInfo(
    val trackId: String,
    val trainId: String,
    val startedAt: Long,
    val endedAt: Long,
    val isFavorite: Boolean,
)
