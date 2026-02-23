package com.z_company.domain.use_cases

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.fullRest
import com.z_company.domain.entities.route.UtilsForEntities.shortRest
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.util.lessThan
import com.z_company.domain.util.moreThan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
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
            val tz = TimeZone.currentSystemDefault()
            val startDate = LocalDate(monthOfYear.year, monthOfYear.month + 1, 1)
            val startMonthInLong = startDate.atStartOfDayIn(tz).toEpochMilliseconds() - offsetInMoscow
            val maxDayOfMonth = startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth

            val endMonthInLong = LocalDateTime(
                monthOfYear.year, monthOfYear.month + 1, maxDayOfMonth, 23, 59, 0, 0
            ).toInstant(tz).toEpochMilliseconds() - offsetInMoscow

            repository.loadRouteByPeriodFlow(
                startPeriod = startMonthInLong,
                endPeriod = endMonthInLong
            ).collect { routes ->
                trySend(routes)
            }
            awaitClose()
        }
    }


    fun listRoutesByMonth(
        monthOfYear: MonthOfYear,
        offsetInMoscow: Long
    ): Flow<ResultState<List<Route>>> =
        channelFlow {
            trySend(ResultState.Loading())

            val tz = TimeZone.currentSystemDefault()
            val startDate = LocalDate(monthOfYear.year, monthOfYear.month + 1, 1)
            val startMonthInLong = startDate.atStartOfDayIn(tz).toEpochMilliseconds() - offsetInMoscow
            val maxDayOfMonth = startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth

            val endMonthInLong = LocalDateTime(
                monthOfYear.year, monthOfYear.month + 1, maxDayOfMonth, 23, 59, 0, 0
            ).toInstant(tz).toEpochMilliseconds() - offsetInMoscow

            withContext(Dispatchers.Default) {
                this.launch {
                    repository.loadRoutesByPeriod(startMonthInLong, endMonthInLong)
                        .collect { result ->
                            if (result is ResultState.Success) {
                                trySend(ResultState.Success(result.data))
                            }
                            if (result is ResultState.Error) {
                                trySend(ResultState.Error(ErrorEntity(result.entity.throwable)))
                            }
                        }
                }
            }
            awaitClose()
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
        return repository.markAsRemoved(route)
    }

    fun saveRoute(route: Route): Flow<ResultState<Unit>> {
        return if (route.basicData.timeStartWork == null) {
            val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()
            repository.saveRoute(
                route.copy(
                    basicData = route.basicData.copy(
                        timeStartWork = currentTimeInMillis,
                        isSynchronized = false
                    )
                )
            )
        } else {
            repository.saveRoute(route.copy(basicData = route.basicData.copy(isSynchronized = false)))
        }
    }

    fun saveRouteAfterLoading(route: Route): Flow<ResultState<Unit>> {
        return repository.saveRoute(route)
    }

    fun setSynchronizedRoute(basicId: String): Flow<ResultState<Unit>> {
        return repository.setSynchronizedRoute(basicId)
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

    fun setRemoteObjectIdTrain(trainId: String, objectId: String): Flow<ResultState<Unit>> {
        return repository.setRemoteObjectIdTrain(trainId, objectId)
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
            trySend(ResultState.Success(Unit))
            awaitClose()
        }
    }

    fun isValidPassenger(route: Route): Flow<ResultState<Unit>> {
        return channelFlow {
            val startTime = route.basicData.timeStartWork
            val endTime = route.basicData.timeEndWork

            route.passengers.forEach { passenger ->
                if (passenger.timeArrival.lessThan(passenger.timeDeparture)) {
                    trySend(
                        ResultState.Error(
                            ErrorEntity(message = "Прибытие пассажиром раньше отправления. Невозможно сохранить данные.")
                        )
                    )
                }
                if (passenger.timeDeparture.moreThan(endTime)) {
                    trySend(
                        ResultState.Error(
                            ErrorEntity(message = "Отправление пассажиром позже окончания работы. Невозможно сохранить данные.")
                        )
                    )
                }
                if (passenger.timeDeparture.lessThan(startTime)) {
                    trySend(
                        ResultState.Error(
                            ErrorEntity(message = "Отправление пассажиром раньше начала работы. Невозможно сохранить данные.")
                        )
                    )
                }
                if (passenger.timeArrival.moreThan(endTime)) {
                    trySend(
                        ResultState.Error(
                            ErrorEntity(message = "Прибытие пассажиром позже окончания работы. Невозможно сохранить данные.")
                        )
                    )
                }
                if (passenger.timeArrival.lessThan(startTime)) {
                    trySend(
                        ResultState.Error(
                            ErrorEntity(message = "Прибытие пассажиром раньше начала работы. Невозможно сохранить данные.")
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
}
