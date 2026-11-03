package com.z_company.domain.track

/**
 * Согласие №1 для отправки треков: что на устройстве и что подтвердил сервер
 * (сервер проверяет флаг на каждой записи, клиентскому не доверяет — SCREEN_SPECS §17.7).
 */
interface TrackSyncSettings {
    /** Согласие «Сохранять мои треки и историю поездок» на устройстве. */
    fun localConsent(): Boolean

    /** Состояние согласия, подтверждённое сервером; null — неизвестно (нужно отправить `PUT /tracks/consent`). */
    fun serverConsent(): Boolean?
    fun setServerConsent(value: Boolean?)
}

/** К какому рейсу относится поезд трека и как подписать карточку. */
sealed interface TrackTrainResolution {
    data class Found(val routeId: String, val title: String) : TrackTrainResolution

    /** Поезда нет ни в одном рейсе (удалён): трек осиротел. */
    data object Missing : TrackTrainResolution

    /** Не удалось определить (ошибка чтения БД): ничего не делать с треком. */
    data object Unknown : TrackTrainResolution
}

fun interface TrackTrainResolver {
    suspend fun resolve(trainId: String): TrackTrainResolution
}

/**
 * Очистка осиротевших локальных треков: поезд которых уже нет ни в одном рейсе (удалён рейс целиком,
 * серверный full-replace убрал поезд, рейс удалён на другом устройстве). Удаление ставится в очередь и для сервера.
 */
object TrackOrphanCleaner {
    /** Фоновая очистка: свежие треки не трогаем, поезд мог быть ещё не сохранён в рейсе. */
    const val GRACE_MS = 24 * 60 * 60_000L

    /**
     * Очистка при открытии «Моих треков»: удаление поезда/рейса теперь явно удаляет его треки, поэтому
     * сирота здесь — результат сбоя/синхронизации. Защищаем только трек, который пишется прямо сейчас.
     */
    const val EXPLICIT_GRACE_MS = 10 * 60_000L

    /**
     * Треки для показа в «Моих треках»: без сирот (поезда нет ни в одном рейсе). Пустое множество — «не удалось
     * прочитать рейсы», тогда показываем всё, как и [clean] ничего не удаляет.
     */
    fun visible(tracks: List<TrackInfo>, existingTrainIds: Set<String>): List<TrackInfo> =
        if (existingTrainIds.isEmpty()) tracks
        else tracks.filter { it.trainId == NO_TRAIN_ID || it.trainId in existingTrainIds }

    /**
     * @param existingTrainIds идентификаторы ВСЕХ поездов во ВСЕХ рейсах, включая корзину и ожидающие удаления.
     *   Пустое множество трактуется как «не удалось прочитать» — ничего не удаляем (защита от потери треков).
     * @return сколько треков удалено
     */
    fun clean(
        repository: TrackRepository,
        existingTrainIds: Set<String>,
        nowMs: Long,
        graceMs: Long = GRACE_MS,
    ): Int {
        if (existingTrainIds.isEmpty()) return 0
        val orphans = repository.tracks().filter {
            it.trainId != NO_TRAIN_ID && it.trainId !in existingTrainIds && it.endedAt < nowMs - graceMs
        }
        orphans.forEach { repository.deleteTrack(it.trackId, queueRemoteDelete = true) }
        return orphans.size
    }
}
