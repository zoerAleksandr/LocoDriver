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
    fun track(trackId: String): TrackInfo?

    /** Поставить/снять звезду; помечает изменение как не доставленное на сервер. */
    fun setFavorite(trackId: String, favorite: Boolean)
    fun clearFavoriteDirty(trackId: String)

    /** Записать результат отправки ([TrackUploadState]) и число отправленных точек. */
    fun setUploadState(trackId: String, state: Int, uploadedPoints: Long)

    /**
     * Удалить один трек с устройства. [queueRemoteDelete] — поставить в очередь удаление и на сервере
     * (доставит [TrackUploader]; DELETE идемпотентен).
     */
    fun deleteTrack(trackId: String, queueRemoteDelete: Boolean)

    /** Идентификаторы треков, удаление которых ещё не доставлено на сервер. */
    fun pendingRemoteDeletes(): List<String>
    fun clearPendingRemoteDelete(trackId: String)

    /** Сбросить метки отправки (все треки снова «ожидают отправки»). Отзыв согласия его не вызывает. */
    fun resetUploadState()

    /**
     * Удалить все треки поезда (при удалении поезда/рейса); удаление ставится в очередь и для сервера.
     * Поезд помечается удалённым: пока идёт запись «АВТО» с запомненной целью, новые точки в него не пишутся
     * (иначе писатель заново создал бы только что удалённый трек). Метка живёт до перезапуска процесса.
     */
    fun deleteByTrain(trainId: String)

    /** Снять метку «поезд удалён» (рейс восстановлен из корзины); удалённые треки не возвращаются. */
    fun allowTrain(trainId: String)

    /** Удалить все локальные треки. */
    fun deleteAll()

    /** Удалить просроченные не избранные треки; вернуть, сколько удалено. */
    fun purgeExpired(nowMs: Long): Int
}
