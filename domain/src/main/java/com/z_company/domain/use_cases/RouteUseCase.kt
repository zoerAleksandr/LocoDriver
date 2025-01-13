package com.z_company.domain.use_cases

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.fullRest
import com.z_company.domain.entities.route.UtilsForEntities.isTimeWorkValid
import com.z_company.domain.entities.route.UtilsForEntities.shortRest
import com.z_company.domain.repositories.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Calendar.*

class RouteUseCase(private val repository: RouteRepository) {
    suspend fun listRoutesByMonth(monthOfYear: MonthOfYear): Flow<ResultState<List<Route>>> =
        channelFlow {
            trySend(ResultState.Loading)

            val startMonth: Calendar = getInstance().also {
                it.set(YEAR, monthOfYear.year)
                it.set(MONTH, monthOfYear.month)
                it.set(DAY_OF_MONTH, 1)
                it.set(HOUR_OF_DAY, 0)
                it.set(MINUTE, 0)
                it.set(SECOND, 0)
                it.set(MILLISECOND, 0)
            }
            // TODO
            val startMonthInLong: Long = startMonth.timeInMillis - 14_400_000
            val maxDayOfMonth = startMonth.getActualMaximum(DAY_OF_MONTH)

            val endMonth: Calendar = getInstance().also {
                it.set(YEAR, monthOfYear.year)
                it.set(MONTH, monthOfYear.month)
                it.set(DAY_OF_MONTH, maxDayOfMonth)
                it.set(HOUR_OF_DAY, 23)
                it.set(MINUTE, 59)
                it.set(SECOND, 0)
                it.set(MILLISECOND, 0)
            }
            // TODO
            val endMonthInLong = endMonth.timeInMillis - 14_400_000

            withContext(Dispatchers.IO) {
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
        }

    fun listRouteWithDeleting(): List<Route> {
        return repository.loadRoutesWithDeleting()
    }


    fun getListRoutes(): List<Route> {
        return repository.loadRoutes()
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
            val currentTimeInMillis = getInstance().timeInMillis
            repository.saveRoute(route.copy(basicData = route.basicData.copy(timeStartWork = currentTimeInMillis, isSynchronizedRoute = false)))
        } else {
            repository.saveRoute(route.copy(basicData = route.basicData.copy(isSynchronizedRoute = false)))
        }
    }

    fun setSynchronizedRoute(basicId: String): Flow<ResultState<Unit>> {
        return repository.setSynchronizedRoute(basicId)
    }

    fun setRemoteObjectIdRoute(
        basicId: String,
        remoteObjectId: String?
    ): Flow<ResultState<Unit>> {
        return repository.setRemoteObjectIdRoute(basicId, remoteObjectId)
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

    private fun isRouteValid(route: Route): Boolean {
        return route.basicData.timeStartWork != null
    }

    fun isTimeWorkValid(route: Route): Boolean {
        return route.isTimeWorkValid()
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
}