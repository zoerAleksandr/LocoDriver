@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.use_cases

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.UtilsForEntities.filterByMonth
import com.z_company.domain.entities.route.UtilsForEntities.fullRest
import com.z_company.domain.entities.route.UtilsForEntities.monthBoundsMillis
import com.z_company.domain.entities.route.UtilsForEntities.shortRest
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.lessThan
import com.z_company.domain.util.moreThan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant


class RouteUseCase(private val repository: RouteRepository) {
    fun routeListByMonthFlow(monthOfYear: MonthOfYear, offsetInMoscow: Long): Flow<List<Route>> {
        return callbackFlow {
            // Границы месяца всегда в московском времени (GMT+3): пользователь вводит времена в МСК
            val moscowTZ = TimeZone.of("GMT+3")
            val startDate = LocalDate(monthOfYear.year, monthOfYear.month + 1, 1)
            val startMonthInLong = startDate.atStartOfDayIn(moscowTZ).toEpochMilliseconds()
            val maxDayOfMonth = startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
            val endMonthInLong = LocalDateTime(
                monthOfYear.year, monthOfYear.month + 1, maxDayOfMonth, 23, 59, 0, 0
            ).toInstant(moscowTZ).toEpochMilliseconds()
            // Расширяем начало на 2 дня чтобы захватить переходные маршруты из предыдущего месяца
            val extendedStart = startMonthInLong - 2 * 24 * 3_600_000L

            repository.loadRouteByPeriodFlow(
                startPeriod = extendedStart,
                endPeriod = endMonthInLong
            ).collect { routes ->
                // Пост-фильтр: оставляем только маршруты, реально пересекающиеся с месяцем.
                // SQL-запрос getByPeriod использует OR-условия по timeEndWork и может захватить
                // маршруты из прошлого месяца (например, сдача попадает в расширенный период).
                val filtered = routes.filter { route ->
                    val start = route.basicData.timeStartWork ?: return@filter true
                    val end = route.basicData.timeEndWork
                    start < endMonthInLong && (end == null || end >= startMonthInLong)
                }
                trySend(filtered)
            }
            awaitClose()
        }
    }


    fun routeListByMonthFlow(monthOfYear: MonthOfYear, context: TimeCalculationContext): Flow<List<Route>> {
        return callbackFlow {
            val (startMonthInLong, endMonthInLong) = monthBoundsMillis(monthOfYear, context)
            val extendedStart = startMonthInLong - 2 * 24 * 3_600_000L

            repository.loadRouteByPeriodFlow(
                startPeriod = extendedStart,
                endPeriod = endMonthInLong
            ).collect { routes ->
                // Пост-фильтр общий с виджетом — см. UtilsForEntities.filterByMonth
                trySend(routes.filterByMonth(monthOfYear, context))
            }
            awaitClose()
        }
    }

    fun listRoutesByMonth(
        monthOfYear: MonthOfYear,
        offsetInMoscow: Long
    ): Flow<ResultState<List<Route>>> = flow {
        emit(ResultState.Loading())

        val moscowTZ = TimeZone.of("GMT+3")
        val startDate = LocalDate(monthOfYear.year, monthOfYear.month + 1, 1)
        val startMonthInLong = startDate.atStartOfDayIn(moscowTZ).toEpochMilliseconds()
        val maxDayOfMonth = startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
        val endMonthInLong = LocalDateTime(
            monthOfYear.year, monthOfYear.month + 1, maxDayOfMonth, 23, 59, 0, 0
        ).toInstant(moscowTZ).toEpochMilliseconds()
        val extendedStart = startMonthInLong - 2 * 24 * 3_600_000L

        emitAll(
            repository.loadRoutesByPeriod(extendedStart, endMonthInLong)
                .map { state ->
                    if (state is ResultState.Success) {
                        ResultState.Success(state.data.filter { route ->
                            val start = route.basicData.timeStartWork ?: return@filter true
                            val end = route.basicData.timeEndWork
                            start < endMonthInLong && (end == null || end >= startMonthInLong)
                        })
                    } else state
                }
        )
    }

    fun listRoutesByMonth(
        monthOfYear: MonthOfYear,
        context: TimeCalculationContext
    ): Flow<ResultState<List<Route>>> = flow {
        emit(ResultState.Loading())

        val tz = context.crossMonthTZ
        val startDate = LocalDate(monthOfYear.year, monthOfYear.month + 1, 1)
        val startMonthInLong = startDate.atStartOfDayIn(tz).toEpochMilliseconds()
        val maxDayOfMonth = startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
        val endMonthInLong = LocalDateTime(
            monthOfYear.year, monthOfYear.month + 1, maxDayOfMonth, 23, 59, 0, 0
        ).toInstant(tz).toEpochMilliseconds()
        val extendedStart = startMonthInLong - 2 * 24 * 3_600_000L

        emitAll(
            repository.loadRoutesByPeriod(extendedStart, endMonthInLong)
                .map { state ->
                    if (state is ResultState.Success) {
                        ResultState.Success(state.data.filter { route ->
                            val start = route.basicData.timeStartWork ?: return@filter true
                            val end = route.basicData.timeEndWork
                            start < endMonthInLong && (end == null || end >= startMonthInLong)
                        })
                    } else state
                }
        )
    }

    fun listRouteWithDeleting(): List<Route> {
        return repository.loadRoutesWithDeleting()
    }


    fun getListRoutes(): List<Route> {
        return repository.loadRoutes()
    }

    fun getListRoutesAsStateFlow(): Flow<ResultState<List<Route>>> {
        return repository.loadRoutesAsStateFlow()
    }
    fun getListRoutesAsFlow(): Flow<List<Route>> {
        return repository.loadRoutesAsFlow()
    }

    fun routeDetails(routeId: String): Flow<ResultState<Route?>> {
        return repository.loadRoute(routeId)
    }

    fun removeRoute(route: Route): Flow<ResultState<Unit>> {
        return repository.remove(route)
    }

    fun markAsRemoved(route: Route): Flow<ResultState<Unit>> {
        // Штампуем updatedAt удаления — время soft-delete участвует в LWW-merge
        // (удаление на этом устройстве побеждает более старую правку на другом).
        val now = Clock.System.now().toEpochMilliseconds()
        return repository.markAsRemoved(
            route.copy(basicData = route.basicData.copy(updatedAt = now))
        )
    }

    fun saveRoute(route: Route): Flow<ResultState<Unit>> {
        // Инвариант «явки по прибытию пассажиром»: если какой-то пассажир помечен
        // как источник начала работы (isWorkStartByArrival) и у него задано время
        // прибытия — timeStartWork маршрута всегда равен этому времени. Централизуем
        // здесь, чтобы правило соблюдалось на всех путях сохранения (FormViewModel,
        // экран «Пассажиром», WorkSchedule и т.д.), а не только при переключении.
        val normalizedRoute = applyWorkStartByArrival(route)
        // Штамп updatedAt на КАЖДОМ локальном изменении — это часы для LWW-merge
        // в двусторонней синхронизации. Локальная правка всегда двигает updatedAt
        // вперёд, поэтому при конфликте с сервером побеждает более свежая версия.
        // ВАЖНО: сохранение маршрута, ПРИШЕДШЕГО с сервера, идёт через
        // saveRouteAfterLoading (updatedAt там сохраняется серверный, не трогаем).
        val now = Clock.System.now().toEpochMilliseconds()
        return if (normalizedRoute.basicData.timeStartWork == null) {
            repository.saveRoute(
                normalizedRoute.copy(
                    basicData = normalizedRoute.basicData.copy(
                        timeStartWork = now,
                        isSynchronized = false,
                        updatedAt = now
                    )
                )
            )
        } else {
            repository.saveRoute(
                normalizedRoute.copy(
                    basicData = normalizedRoute.basicData.copy(
                        isSynchronized = false,
                        updatedAt = now
                    )
                )
            )
        }
    }

    /**
     * Приводит [Route.basicData]`.timeStartWork` к времени прибытия пассажира,
     * помеченного [Passenger.isWorkStartByArrival]. Если таких нет — возвращает
     * маршрут без изменений. Берётся первый подходящий пассажир (флаг взаимо-
     * исключающий, выставляется на экране «Пассажиром»).
     */
    private fun applyWorkStartByArrival(route: Route): Route {
        val workStart = route.passengers.firstOrNull {
            it.isWorkStartByArrival && it.timeArrival != null
        } ?: return route
        if (route.basicData.timeStartWork == workStart.timeArrival) return route
        return route.copy(
            basicData = route.basicData.copy(timeStartWork = workStart.timeArrival)
        )
    }

    fun saveRouteAfterLoading(route: Route): Flow<ResultState<Unit>> {
        // Очистка данных после загрузки из облака
        val cleanedRoute = route.copy(
            trains = route.trains.map { train ->
                train.copy(
                    stations = sortStations(
                        train.stations.map { station ->
                            station.copy(
                                // Python-сервер конвертирует null → "None"
                                stationName = if (station.stationName == "None") null else station.stationName
                            )
                        }
                    ).toMutableList()
                )
            }.toMutableList()
        )
        return repository.saveRoute(cleanedRoute)
    }

    /**
     * Восстанавливает порядок станций после загрузки из облака.
     * Приоритет: orderIndex (если хотя бы у одной станции != 0),
     * иначе — по времени (отправление, затем прибытие).
     * Станции без времени сохраняют относительный порядок.
     */
    private fun sortStations(stations: List<Station>): List<Station> {
        if (stations.size <= 1) return stations

        val hasOrderIndex = stations.any { it.orderIndex != 0 }
        if (hasOrderIndex) {
            return stations.sortedBy { it.orderIndex }
        }

        // Сортируем по времени: берём самое раннее время каждой станции
        val withTime = stations.filter { it.timeDeparture != null || it.timeArrival != null }
        val withoutTime = stations.filter { it.timeDeparture == null && it.timeArrival == null }

        val sorted = withTime.sortedBy { it.timeDeparture ?: it.timeArrival ?: Long.MAX_VALUE }
        return sorted + withoutTime
    }

    fun setSynchronizedRoute(basicId: String): Flow<ResultState<Unit>> {
        return repository.setSynchronizedRoute(basicId)
    }

    fun markUnsynchronized(basicId: String): Flow<ResultState<Unit>> {
        return repository.markUnsynchronized(basicId)
    }

    fun setRemoteRouteIdRoute(
        basicId: String,
        remoteRouteId: String?
    ): Flow<ResultState<Unit>> {
        return repository.setRemoteObjectIdRoute(basicId, remoteRouteId)
    }

    fun setRemoteObjectIdBasicData(
        basicId: String,
        remoteObjectId: String?
    ): Flow<ResultState<Unit>> {
        return repository.setRemoteObjectIdBasicData(basicId, remoteObjectId)
    }

    fun setRemoteObjectIdLocomotive(
        locoId: String,
        remoteObjectId: String
    ): Flow<ResultState<Unit>> {
        return repository.setRemoteObjectIdLocomotive(locoId, remoteObjectId)
    }

    fun setRemoteObjectIdPassenger(passengerId: String, objectId: String): Flow<ResultState<Unit>> {
        return repository.setRemoteObjectIdPassenger(passengerId, objectId)
    }

    fun setRemoteObjectIdPhoto(photoId: String, objectId: String): Flow<ResultState<Unit>> {
        return repository.setRemoteObjectIdPhoto(photoId, objectId)
    }

    fun getPhotoById(photoId: String): Flow<ResultState<Photo?>> {
        return repository.loadPhoto(photoId)
    }

    fun isRouteValid(route: Route): Flow<ResultState<Unit>> {
        return channelFlow {
            val flows = listOf(
                isValidBasicData(route),
                isValidTrain(route),
                isValidPassenger(route),
            )
            combine(
                isValidBasicData(route),
                isValidTrain(route),
                isValidPassenger(route)
            ) { arrayResult ->
                arrayResult.map { result ->
                    println("ZZZ result $result")
                    if (result is ResultState.Error) {
                        trySend(ResultState.Error(result.entity))
                    }
                }
            }.collect {}
            trySend(ResultState.Success(Unit))
            awaitClose()
        }
    }

    fun isValidBasicData(route: Route): Flow<ResultState<Unit>> {
        return channelFlow {
            val startTime = route.basicData.timeStartWork
            val endTime = route.basicData.timeEndWork
            if (startTime.moreThan(endTime)) {
                trySend(
                    ResultState.Error(
                        ErrorEntity(message = "Начало работы позже окончания. Невозможно сохранить маршрут.")
                    )
                )
            }
            val breakStart = route.basicData.timeStartBreak
            val breakEnd = route.basicData.timeEndBreak
            if (breakStart != null && breakEnd != null) {
                if (breakStart >= breakEnd) {
                    trySend(
                        ResultState.Error(
                            ErrorEntity(message = "Начало перерыва позже или равно окончанию.")
                        )
                    )
                }
                if (startTime != null && breakStart < startTime) {
                    trySend(
                        ResultState.Error(
                            ErrorEntity(message = "Начало перерыва раньше явки.")
                        )
                    )
                }
                if (endTime != null && breakEnd > endTime) {
                    trySend(
                        ResultState.Error(
                            ErrorEntity(message = "Окончание перерыва позже сдачи.")
                        )
                    )
                }
            }
            trySend(ResultState.Success(Unit))
            awaitClose()
        }
    }

    fun isValidPassenger(route: Route): Flow<ResultState<Unit>> {
        return channelFlow {
            route.passengers.forEach { passenger ->
                // Единственная жёсткая ошибка — прибытие раньше отправления (невозможная поездка).
                // Следование ВНЕ окна смены [явка, сдача] теперь допускается: оно просто не
                // входит в оплату (пользователь видит информационный баннер на экране «Пассажиром»),
                // поэтому блокирующих проверок «раньше начала / позже окончания» больше нет.
                if (passenger.timeArrival.lessThan(passenger.timeDeparture)) {
                    trySend(
                        ResultState.Error(
                            ErrorEntity(message = "Прибытие пассажиром раньше отправления. Невозможно сохранить данные.")
                        )
                    )
                }
            }
            trySend(ResultState.Success(Unit))
            awaitClose()
        }
    }

    fun isValidTrain(route: Route): Flow<ResultState<Unit>> {
        return channelFlow {
            route.trains.forEach { train ->
                train.stations.forEachIndexed { index, station ->
                    val name = station.stationName ?: (index + 1)
                    if (station.timeDeparture.lessThan(route.basicData.timeStartWork)) {
                        trySend(
                            ResultState.Error(
                                ErrorEntity(message = "Станция $name. Отправление раньше начала работы. Невозможно сохранить данные.")
                            )
                        )
                    }
                    if (station.timeArrival.lessThan(route.basicData.timeStartWork)) {
                        trySend(
                            ResultState.Error(
                                ErrorEntity(message = "Станция $name. Прибытие раньше начала работы. Невозможно сохранить данные.")
                            )
                        )
                    }
                    if (station.timeDeparture.moreThan(route.basicData.timeEndWork)) {
                        trySend(
                            ResultState.Error(
                                ErrorEntity(message = "Станция $name. Отправление позже окончания работы. Невозможно сохранить данные.")
                            )
                        )
                    }
                    if (station.timeArrival.moreThan(route.basicData.timeEndWork)) {
                        trySend(
                            ResultState.Error(
                                ErrorEntity(message = "Станция $name. Прибытие позже окончания работы. Невозможно сохранить данные.")
                            )
                        )
                    }
                    if (station.timeArrival.moreThan(station.timeDeparture)) {
                        trySend(
                            ResultState.Error(
                                ErrorEntity(message = "Станция $name. Прибытие позже отправления. Невозможно сохранить данные.")
                            )
                        )
                    }
                    if (index != 0) {
                        val previousStation = train.stations[index - 1]
                        if (station.timeDeparture.lessThan(previousStation.timeArrival)) {
                            trySend(
                                ResultState.Error(
                                    ErrorEntity(message = "Отправление со станции $name раньше прибытия на станцию ${previousStation.stationName}. Невозможно сохранить данные.")
                                )
                            )
                        }
                        if (station.timeArrival.lessThan(previousStation.timeDeparture)) {
                            trySend(
                                ResultState.Error(
                                    ErrorEntity(message = "Прибытие на станцию $name раньше отправления со станции ${previousStation.stationName}. Невозможно сохранить данные.")
                                )
                            )
                        }
                    }
                }
            }
            trySend(ResultState.Success(Unit))
            awaitClose()
        }
    }

    fun isTimeWorkValid(route: Route): Boolean {
        val startTime = route.basicData.timeStartWork
        val endTime = route.basicData.timeEndWork

        return !startTime.moreThan(endTime)
    }

    fun getMinRest(route: Route, minTimeRest: Long?): Long? {
        return route.shortRest(minTimeRest)
    }

    fun fullRest(route: Route, minTimeRest: Long?): Long? {
        return route.fullRest(minTimeRest)
    }

    fun clearLocalRouteRepository(): Flow<ResultState<Unit>> {
        return repository.clearRepository()
    }

    fun setFavoriteRoute(routeId: String, isFavorite: Boolean): Flow<ResultState<Boolean>> {
        return repository.setFavoriteRoute(routeId, isFavorite)
    }

    /**
     * Одноразовая миграция: сдвигает все timestamp-поля маршрутов на [offsetFromMoscow] мс.
     *
     * Необходима для пользователей из регионов, отличных от Москвы, которые вводили данные
     * до переключения отображения на московское время (GMT+3). До исправления DateAndTimeConverter
     * использовал часовой пояс телефона, поэтому, например, пользователь в Иркутске (UTC+8)
     * вводя "21:20" сохранял 13:20 UTC вместо правильных 18:20 UTC.
     *
     * Формула: newEpoch = oldEpoch + offsetFromMoscow
     * - Москва (offset=0): no-op, данные не изменяются
     * - Иркутск (+5ч = 18_000_000 мс): 13:20 UTC + 5ч = 18:20 UTC → отображается как "21:20 MSK" ✓
     */
    suspend fun migrateTimestamps(offsetFromMoscow: Long): Unit = withContext(Dispatchers.Default) {
        if (offsetFromMoscow == 0L) return@withContext

        val routes = repository.loadRoutes()
        routes.forEach { route ->
            val migratedRoute = route.copy(
                basicData = route.basicData.copy(
                    timeStartWork = route.basicData.timeStartWork?.plus(offsetFromMoscow),
                    timeEndWork = route.basicData.timeEndWork?.plus(offsetFromMoscow),
                    timeStartBreak = route.basicData.timeStartBreak?.plus(offsetFromMoscow),
                    timeEndBreak = route.basicData.timeEndBreak?.plus(offsetFromMoscow),
                ),
                locomotives = route.locomotives.map { loco ->
                    loco.copy(
                        timeStartOfAcceptance = loco.timeStartOfAcceptance?.plus(offsetFromMoscow),
                        timeEndOfAcceptance = loco.timeEndOfAcceptance?.plus(offsetFromMoscow),
                        timeStartOfDelivery = loco.timeStartOfDelivery?.plus(offsetFromMoscow),
                        timeEndOfDelivery = loco.timeEndOfDelivery?.plus(offsetFromMoscow),
                    )
                }.toMutableList(),
                trains = route.trains.map { train ->
                    train.copy(
                        stations = train.stations.map { station ->
                            station.copy(
                                timeArrival = station.timeArrival?.plus(offsetFromMoscow),
                                timeDeparture = station.timeDeparture?.plus(offsetFromMoscow),
                            )
                        }.toMutableList()
                    )
                }.toMutableList(),
                passengers = route.passengers.map { passenger ->
                    passenger.copy(
                        timeArrival = passenger.timeArrival?.plus(offsetFromMoscow),
                        timeDeparture = passenger.timeDeparture?.plus(offsetFromMoscow),
                    )
                }.toMutableList(),
            )
            repository.saveRoute(migratedRoute).collect {}
        }
    }
}
