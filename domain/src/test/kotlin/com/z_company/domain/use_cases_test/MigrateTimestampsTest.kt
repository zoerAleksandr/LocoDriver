package com.z_company.domain.use_cases_test

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.*
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Тесты для RouteUseCase.migrateTimestamps.
 *
 * Контекст: до исправления DateAndTimeConverter использовал часовой пояс телефона.
 * Пользователь в Иркутске (UTC+8, offsetFromMoscow=18_000_000мс=5ч) вводя "21:20 MSK"
 * сохранял 21:20 Иркутск = 13:20 UTC вместо правильных 18:20 UTC.
 * Миграция: newEpoch = oldEpoch + offsetFromMoscow → 13:20 UTC + 5ч = 18:20 UTC ✓
 */
class MigrateTimestampsTest {

    // ==================== Фейковый репозиторий ====================

    private class FakeRouteRepository(initialRoutes: List<Route> = emptyList()) : RouteRepository {
        private val routes = initialRoutes.toMutableList()
        val savedRoutes = mutableListOf<Route>()

        override fun loadRoutes(): List<Route> = routes.toList()
        override fun saveRoute(route: Route): Flow<ResultState<Unit>> = flow {
            savedRoutes.add(route)
            emit(ResultState.Success(Unit))
        }

        // Остальные методы не используются в тестах
        override fun loadRoutesWithDeleting(): List<Route> = emptyList()
        override fun loadRoute(routeId: String): Flow<ResultState<Route?>> = flow { emit(ResultState.Success(null)) }
        override fun loadLoco(locoId: String): Flow<ResultState<Locomotive?>> = flow { emit(ResultState.Success(null)) }
        override fun loadLocoListByBasicId(basicId: String): List<Locomotive> = emptyList()
        override fun loadTrain(trainId: String): Flow<ResultState<Train?>> = flow { emit(ResultState.Success(null)) }
        override fun loadTrainListByBasicId(basicId: String): List<Train> = emptyList()
        override fun loadPassenger(passengerId: String): Flow<ResultState<Passenger?>> = flow { emit(ResultState.Success(null)) }
        override fun loadPassengerListByBasicId(basicId: String): List<Passenger> = emptyList()
        override fun loadPhoto(photoId: String): Flow<ResultState<Photo?>> = flow { emit(ResultState.Success(null)) }
        override fun loadPhotosByRoute(basicId: String): Flow<ResultState<List<Photo>>> = flow { emit(ResultState.Success(emptyList())) }
        override fun remove(route: Route): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun removeLoco(locomotive: Locomotive): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun removeTrain(train: Train): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun removePassenger(passenger: Passenger): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun removePhoto(photo: Photo): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun markAsRemoved(route: Route): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun saveTrain(train: Train): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun updateTrain(train: Train): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun savePassenger(passenger: Passenger): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun savePhoto(photo: Photo): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun loadRoutesByPeriod(startPeriod: Long, endPeriod: Long): Flow<ResultState<List<Route>>> = flow { emit(ResultState.Success(emptyList())) }
        override fun loadRoutesAsStateFlow(): Flow<ResultState<List<Route>>> = flow { emit(ResultState.Success(emptyList())) }
        override fun loadRoutesAsFlow(): Flow<List<Route>> = flow { emit(emptyList()) }
        override fun loadRouteByPeriodFlow(startPeriod: Long, endPeriod: Long): Flow<List<Route>> = flow { emit(emptyList()) }
        override fun setSynchronizedRoute(basicId: String): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun setRemoteObjectIdRoute(basicId: String, remoteRouteId: String?): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun setRemoteObjectIdBasicData(basicId: String, remoteObjectId: String?): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun setRemoteObjectIdLocomotive(locoId: String, remoteObjectId: String): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun setRemoteObjectIdPassenger(passengerId: String, objectId: String): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun setRemoteObjectIdPhoto(photoId: String, objectId: String): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun clearRepository(): Flow<ResultState<Unit>> = flow { emit(ResultState.Success(Unit)) }
        override fun setFavoriteRoute(routeId: String, isFavorite: Boolean): Flow<ResultState<Boolean>> = flow { emit(ResultState.Success(isFavorite)) }
    }

    // ==================== Константы ====================

    private val oneHour = 3_600_000L
    private val offsetIrkutsk = 5 * oneHour       // UTC+8, +5ч от Москвы
    private val offsetEkaterinburg = 2 * oneHour   // UTC+5, +2ч от Москвы
    private val offsetMoscow = 0L                  // UTC+3, Москва — без сдвига

    // Произвольный базовый timestamp для тестов (13:20 UTC = 21:20 Иркутск = 16:20 MSK)
    private val baseTimestamp = 1_000_000_000L

    // ==================== BasicData ====================

    @Test
    fun moscowOffset_noRoutes_nothingSaved() = runTest {
        val repo = FakeRouteRepository(emptyList())
        RouteUseCase(repo).migrateTimestamps(offsetMoscow)
        assertEquals(0, repo.savedRoutes.size)
    }

    @Test
    fun moscowOffset_noTimestampShift() = runTest {
        val route = Route(basicData = BasicData(timeStartWork = baseTimestamp, timeEndWork = baseTimestamp + oneHour))
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetMoscow)
        // offset=0 → ранний выход, ничего не сохранено
        assertEquals(0, repo.savedRoutes.size)
    }

    @Test
    fun irkutskOffset_basicData_timesShiftedBy5h() = runTest {
        val route = Route(
            basicData = BasicData(
                timeStartWork = baseTimestamp,
                timeEndWork = baseTimestamp + 8 * oneHour,
                timeStartBreak = baseTimestamp + 2 * oneHour,
                timeEndBreak = baseTimestamp + 3 * oneHour,
            )
        )
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        val saved = repo.savedRoutes.first().basicData
        assertEquals(baseTimestamp + offsetIrkutsk, saved.timeStartWork)
        assertEquals(baseTimestamp + 8 * oneHour + offsetIrkutsk, saved.timeEndWork)
        assertEquals(baseTimestamp + 2 * oneHour + offsetIrkutsk, saved.timeStartBreak)
        assertEquals(baseTimestamp + 3 * oneHour + offsetIrkutsk, saved.timeEndBreak)
    }

    @Test
    fun irkutskOffset_nullTimestamps_stayNull() = runTest {
        val route = Route(
            basicData = BasicData(
                timeStartWork = null,
                timeEndWork = null,
                timeStartBreak = null,
                timeEndBreak = null,
            )
        )
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        val saved = repo.savedRoutes.first().basicData
        assertNull(saved.timeStartWork)
        assertNull(saved.timeEndWork)
        assertNull(saved.timeStartBreak)
        assertNull(saved.timeEndBreak)
    }

    // ==================== Locomotive ====================

    @Test
    fun irkutskOffset_locomotive_allTimesShifted() = runTest {
        val loco = Locomotive(
            basicId = "b1",
            timeStartOfAcceptance = baseTimestamp,
            timeEndOfAcceptance = baseTimestamp + oneHour,
            timeStartOfDelivery = baseTimestamp + 6 * oneHour,
            timeEndOfDelivery = baseTimestamp + 7 * oneHour,
        )
        val route = Route(locomotives = mutableListOf(loco))
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        val savedLoco = repo.savedRoutes.first().locomotives.first()
        assertEquals(baseTimestamp + offsetIrkutsk, savedLoco.timeStartOfAcceptance)
        assertEquals(baseTimestamp + oneHour + offsetIrkutsk, savedLoco.timeEndOfAcceptance)
        assertEquals(baseTimestamp + 6 * oneHour + offsetIrkutsk, savedLoco.timeStartOfDelivery)
        assertEquals(baseTimestamp + 7 * oneHour + offsetIrkutsk, savedLoco.timeEndOfDelivery)
    }

    @Test
    fun irkutskOffset_locomotive_nullTimesStayNull() = runTest {
        val loco = Locomotive(
            basicId = "b1",
            timeStartOfAcceptance = null,
            timeEndOfAcceptance = null,
            timeStartOfDelivery = null,
            timeEndOfDelivery = null,
        )
        val route = Route(locomotives = mutableListOf(loco))
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        val savedLoco = repo.savedRoutes.first().locomotives.first()
        assertNull(savedLoco.timeStartOfAcceptance)
        assertNull(savedLoco.timeEndOfAcceptance)
        assertNull(savedLoco.timeStartOfDelivery)
        assertNull(savedLoco.timeEndOfDelivery)
    }

    // ==================== Station (Train) ====================

    @Test
    fun irkutskOffset_stations_timesShifted() = runTest {
        val station = Station(
            timeArrival = baseTimestamp,
            timeDeparture = baseTimestamp + 30 * 60_000L,
        )
        val train = Train(basicId = "b1", stations = mutableListOf(station))
        val route = Route(trains = mutableListOf(train))
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        val savedStation = repo.savedRoutes.first().trains.first().stations.first()
        assertEquals(baseTimestamp + offsetIrkutsk, savedStation.timeArrival)
        assertEquals(baseTimestamp + 30 * 60_000L + offsetIrkutsk, savedStation.timeDeparture)
    }

    @Test
    fun irkutskOffset_stationNullTimes_stayNull() = runTest {
        val station = Station(timeArrival = null, timeDeparture = null)
        val train = Train(basicId = "b1", stations = mutableListOf(station))
        val route = Route(trains = mutableListOf(train))
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        val savedStation = repo.savedRoutes.first().trains.first().stations.first()
        assertNull(savedStation.timeArrival)
        assertNull(savedStation.timeDeparture)
    }

    // ==================== Passenger ====================

    @Test
    fun irkutskOffset_passenger_timesShifted() = runTest {
        val passenger = Passenger(
            basicId = "b1",
            timeArrival = baseTimestamp,
            timeDeparture = baseTimestamp + 2 * oneHour,
        )
        val route = Route(passengers = mutableListOf(passenger))
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        val saved = repo.savedRoutes.first().passengers.first()
        assertEquals(baseTimestamp + offsetIrkutsk, saved.timeArrival)
        assertEquals(baseTimestamp + 2 * oneHour + offsetIrkutsk, saved.timeDeparture)
    }

    @Test
    fun irkutskOffset_passengerNullTimes_stayNull() = runTest {
        val passenger = Passenger(basicId = "b1", timeArrival = null, timeDeparture = null)
        val route = Route(passengers = mutableListOf(passenger))
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        val saved = repo.savedRoutes.first().passengers.first()
        assertNull(saved.timeArrival)
        assertNull(saved.timeDeparture)
    }

    // ==================== Несколько маршрутов ====================

    @Test
    fun irkutskOffset_multipleRoutes_allShifted() = runTest {
        val routes = listOf(
            Route(basicData = BasicData(timeStartWork = 1_000_000L)),
            Route(basicData = BasicData(timeStartWork = 2_000_000L)),
            Route(basicData = BasicData(timeStartWork = 3_000_000L)),
        )
        val repo = FakeRouteRepository(routes)
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        assertEquals(3, repo.savedRoutes.size)
        assertEquals(1_000_000L + offsetIrkutsk, repo.savedRoutes[0].basicData.timeStartWork)
        assertEquals(2_000_000L + offsetIrkutsk, repo.savedRoutes[1].basicData.timeStartWork)
        assertEquals(3_000_000L + offsetIrkutsk, repo.savedRoutes[2].basicData.timeStartWork)
    }

    // ==================== Другие часовые пояса ====================

    @Test
    fun ekaterinburgOffset_basicData_timesShiftedBy2h() = runTest {
        val route = Route(basicData = BasicData(timeStartWork = baseTimestamp, timeEndWork = baseTimestamp + 8 * oneHour))
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetEkaterinburg)

        val saved = repo.savedRoutes.first().basicData
        assertEquals(baseTimestamp + offsetEkaterinburg, saved.timeStartWork)
        assertEquals(baseTimestamp + 8 * oneHour + offsetEkaterinburg, saved.timeEndWork)
    }

    // ==================== Корректность реального сценария Иркутск ====================

    /**
     * Воспроизводит баг: пользователь в Иркутске (UTC+8) вводил "21:20" по Иркутску.
     * DateAndTimeConverter сохранял это как 13:20 UTC (21:20 - 8ч).
     * После миграции: 13:20 UTC + 5ч (offsetFromMoscow) = 18:20 UTC = 21:20 MSK ✓
     */
    @Test
    fun irkutsk_realScenario_irkutsk2120_becomesUTC1820() = runTest {
        // 13:20 UTC на 31.03.2025 = 1743418800000L (примерное значение)
        // Используем упрощённые числа для наглядности
        val utc13_20 = 13 * oneHour + 20 * 60_000L   // 13:20 UTC как ms от полуночи
        val utc18_20 = 18 * oneHour + 20 * 60_000L   // 18:20 UTC = 21:20 MSK

        val route = Route(basicData = BasicData(timeStartWork = utc13_20))
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        assertEquals(utc18_20, repo.savedRoutes.first().basicData.timeStartWork)
    }

    // ==================== Пустые коллекции ====================

    @Test
    fun irkutskOffset_emptyCollections_savedWithShiftedBasicData() = runTest {
        val route = Route(
            basicData = BasicData(timeStartWork = baseTimestamp),
            locomotives = mutableListOf(),
            trains = mutableListOf(),
            passengers = mutableListOf(),
        )
        val repo = FakeRouteRepository(listOf(route))
        RouteUseCase(repo).migrateTimestamps(offsetIrkutsk)

        val saved = repo.savedRoutes.first()
        assertEquals(baseTimestamp + offsetIrkutsk, saved.basicData.timeStartWork)
        assertEquals(0, saved.locomotives.size)
        assertEquals(0, saved.trains.size)
        assertEquals(0, saved.passengers.size)
    }
}
