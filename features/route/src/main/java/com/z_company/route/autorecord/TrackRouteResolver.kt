package com.z_company.route.autorecord

import com.z_company.domain.entities.route.Route
import com.z_company.domain.track.SpeedScale
import com.z_company.domain.track.TrackOrphanCleaner
import com.z_company.domain.track.TrackPacking
import com.z_company.domain.track.TrackRepository
import com.z_company.domain.track.TrackTrainResolution
import com.z_company.domain.track.TrackTrainResolver
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Связь «поезд трека -> рейс»: рейс нужен серверу (`routeId`, трек удаляется вместе с рейсом), заголовок
 * карточки строится по плечу/станциям поезда. Читает ВСЕ рейсы, включая корзину и ожидающие удаления:
 * сиротой поезд считается, только если его нет нигде (SCREEN_SPECS §6.9).
 */
class TrackRouteResolver(
    private val routeUseCase: RouteUseCase,
    private val trackRepository: TrackRepository,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) : TrackTrainResolver {

    private var cached: Pair<Long, List<Route>>? = null

    /** Полная выборка рейсов тяжёлая; на один прогон отправки хватает снимка не старше минуты. */
    private suspend fun allRoutes(): List<Route>? {
        cached?.let { (at, routes) -> if (nowMs() - at < CACHE_MS) return routes }
        val loaded = withContext(Dispatchers.Default) {
            runCatching { routeUseCase.listRouteWithDeleting() }.getOrNull()
        } ?: return null
        cached = nowMs() to loaded
        return loaded
    }

    /** Сбросить снимок рейсов (после удаления поезда/рейса, перед открытием списка треков). */
    fun invalidate() {
        cached = null
    }

    override suspend fun resolve(trainId: String): TrackTrainResolution {
        val routes = allRoutes() ?: return TrackTrainResolution.Unknown
        for (route in routes) {
            val train = route.trains.firstOrNull { it.trainId == trainId } ?: continue
            // Рейс в корзине ждёт восстановления или удаления: пока ничего не отправляем и не удаляем.
            if (route.basicData.isDeleted) return TrackTrainResolution.Unknown
            return TrackTrainResolution.Found(route.basicData.id, TrackPacking.title(train))
        }
        return TrackTrainResolution.Missing
    }

    /** Пассажирский ли поезд трека (по категории номера); поезд/рейсы не найдены - false. */
    suspend fun isPassengerTrain(trainId: String): Boolean =
        SpeedScale.isPassengerTrain(allRoutes()?.firstNotNullOfOrNull { r -> r.trains.firstOrNull { it.trainId == trainId } })

    /** Явка рейса (`basicData.timeStartWork`) по поезду трека; null — рейса/явки нет. */
    suspend fun workStartByTrain(trainId: String): Long? =
        allRoutes()?.firstOrNull { r -> r.trains.any { it.trainId == trainId } }?.basicData?.timeStartWork

    /** Явка рейса по его id (для серверных треков: у карточки есть только `routeId`). */
    suspend fun workStartByRoute(routeId: String): Long? =
        allRoutes()?.firstOrNull { it.basicData.id == routeId }?.basicData?.timeStartWork

    /** Идентификаторы поездов всех рейсов (включая корзину); null — рейсы прочитать не удалось. */
    suspend fun existingTrainIds(): Set<String>? =
        allRoutes()?.flatMap { r -> r.trains.map { it.trainId } }?.toSet()

    /**
     * Очистка осиротевших треков; безопасна к вызову с любого потока и сколь угодно часто.
     * @param graceMs защита свежих треков; для списка «Мои треки» — [TrackOrphanCleaner.EXPLICIT_GRACE_MS]
     * @return сколько треков удалено
     */
    suspend fun cleanOrphans(graceMs: Long = TrackOrphanCleaner.GRACE_MS): Int {
        val routes = allRoutes() ?: return 0
        val trainIds = routes.flatMap { r -> r.trains.map { it.trainId } }.toSet()
        return withContext(Dispatchers.Default) {
            runCatching { TrackOrphanCleaner.clean(trackRepository, trainIds, nowMs(), graceMs) }.getOrDefault(0)
        }
    }

    private companion object {
        const val CACHE_MS = 60_000L
    }
}
