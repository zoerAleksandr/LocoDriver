package com.z_company.domain.track

/** Срок хранения треков (SCREEN_SPECS §6.9). */
object TrackRetention {
    const val DAYS = 180
    const val MS = DAYS * 24L * 60L * 60L * 1000L

    /** Трек просрочен: не избранный и закончился более [DAYS] дней назад. */
    fun isExpired(endedAtMs: Long, isFavorite: Boolean, nowMs: Long): Boolean =
        !isFavorite && endedAtMs < nowMs - MS
}

/** Локальное хранилище треков. Вызовы блокирующие и быстрые; звать с `Dispatchers.Default`. */
interface TrackRepository {
    /** Дописать точку; трек создаётся при первой точке, `endedAt` двигается вперёд. */
    fun append(trackId: String, trainId: String, point: TrackPoint)
    fun points(trackId: String): List<TrackPoint>
    fun pointCount(trackId: String): Long
    fun tracks(): List<TrackInfo>
    fun tracksForTrain(trainId: String): List<TrackInfo>
    fun setFavorite(trackId: String, favorite: Boolean)

    /** Удалить все треки поезда (при удалении поезда). */
    fun deleteByTrain(trainId: String)

    /** Удалить все локальные треки (отзыв согласия). */
    fun deleteAll()

    /** Удалить просроченные не избранные треки; вернуть, сколько удалено. */
    fun purgeExpired(nowMs: Long): Int
}
