package com.z_company.route.autorecord

import com.z_company.domain.entities.route.Route
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

    /**
     * Очистка осиротевших треков; безопасна к вызову с любого потока и сколь угодно часто.
     * @return сколько треков удалено
     */
    suspend fun cleanOrphans(): Int {
        val routes = allRoutes() ?: return 0
        val trainIds = routes.flatMap { r -> r.trains.map { it.trainId } }.toSet()
        return withContext(Dispatchers.Default) {
            runCatching { TrackOrphanCleaner.clean(trackRepository, trainIds, nowMs()) }.getOrDefault(0)
        }
    }

    private companion object {
        const val CACHE_MS = 60_000L
    }
}
