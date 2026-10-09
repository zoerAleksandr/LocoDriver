package com.z_company.domain.track

/** Точка локального трека (SCREEN_SPECS §6.9). Координаты не логировать. */
data class TrackPoint(
    /** Время фикса, мс от Unix epoch (UTC). */
    val timeMs: Long,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double?,
    val accuracyM: Float?,
    /** Высота по GPS (над уровнем моря, если устройство отдаёт), м; пишется локально, на сервер НЕ уходит (до согласия №2). */
    val altitudeM: Double? = null,
    /** Барометрическая высота (датчик давления, стандартная атмосфера), м; null - датчика нет. Только локально. */
    val baroAltitudeM: Double? = null,
)

/** Заголовок трека: один непрерывный сеанс «АВТО» на одном поезде. */
data class TrackInfo(
    val trackId: String,
    val trainId: String,
    val startedAt: Long,
    val endedAt: Long,
    val isFavorite: Boolean,
    /** Отправка на сервер, [TrackUploadState]. */
    val uploadState: Int = TrackUploadState.PENDING,
    /** Точек в отправленной версии трека (0 — не отправлялся). */
    val uploadedPoints: Long = 0L,
    /** Звезда изменена локально и ещё не доставлена на сервер. */
    val favoriteDirty: Boolean = false,
)

/** Состояние отправки трека на сервер (колонка `Track.uploadState`). */
object TrackUploadState {
    const val PENDING = 0
    const val UPLOADED = 1

    /** Сервер отказал окончательно (квота и т.п.): повторами не исправить, см. [TrackUploader]. */
    const val REJECTED = 2
}
