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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import java.util.Calendar.*

class RouteUseCase(private val repository: RouteRepository) {
    suspend fun listRoutesByMonth(monthOfYear: MonthOfYear): Flow<ResultState<List<Route>>> =
        flow {
            emit(ResultState.Loading)

            val startMonth: Calendar = getInstance().also {
                it.set(YEAR, monthOfYear.year)
                it.set(MONTH, monthOfYear.month)
                it.set(DAY_OF_MONTH, 1)
                it.set(HOUR_OF_DAY, 0)
                it.set(MINUTE, 0)
                it.set(SECOND, 0)
                it.set(MILLISECOND, 0)
            }
            val startMonthInLong: Long = startMonth.timeInMillis
            val maxDayOfMonth = startMonth.getActualMaximum(DAY_OF_MONTH)

            val endMonthInLong: Long = getInstance().also {
                it.set(YEAR, monthOfYear.year)
                it.set(MONTH, monthOfYear.month)
                it.set(DAY_OF_MONTH, maxDayOfMonth)
                it.set(HOUR_OF_DAY, 23)
                it.set(MINUTE, 59)
                it.set(SECOND, 0)
                it.set(MILLISECOND, 0)
            }.timeInMillis

            repository.loadRoutesByPeriod(startMonthInLong, endMonthInLong)
                .collect { result ->
                    if (result is ResultState.Success) {
                        emit(ResultState.Success(result.data))
                    }
                    if (result is ResultState.Error) {
                        emit(ResultState.Error(ErrorEntity(result.entity.throwable)))
                    }
                }

            val startMonthBefore: Calendar = getInstance().also {
                it.set(YEAR, monthOfYear.year)
                it.set(MONTH, monthOfYear.month - 1)
                it.set(DAY_OF_MONTH, 1)
                it.set(HOUR_OF_DAY, 0)
                it.set(MINUTE, 0)
                it.set(SECOND, 0)
                it.set(MILLISECOND, 0)
            }
            val startMonthBeforeInLong = startMonthBefore.timeInMillis
            val maxDayOfMonthBefore = startMonthBefore.getActualMaximum(DAY_OF_MONTH)
            val endMonthBeforeInLong: Long = getInstance().also {
                it.set(YEAR, monthOfYear.year)
                it.set(MONTH, monthOfYear.month - 1)
                it.set(DAY_OF_MONTH, maxDayOfMonthBefore)
                it.set(HOUR_OF_DAY, 23)
                it.set(MINUTE, 59)
                it.set(SECOND, 0)
                it.set(MILLISECOND, 0)
            }.timeInMillis

            repository.loadRoutesByPeriod(startMonthBeforeInLong, endMonthBeforeInLong)
                .collect { result ->
                    if (result is ResultState.Success) {
                        result.data.forEach { route ->
                            route.basicData.timeEndWork?.let { endTime ->
                                if (endTime > startMonthInLong) {
                                    emit(ResultState.Success(result.data))
                                }
                            }
                        }
                    }
                    if (result is ResultState.Error) {
                        emit(ResultState.Error(ErrorEntity(result.entity.throwable)))
                    }
                }
        }

    fun listRoutes(): Flow<ResultState<List<Route>>> {
        return repository.loadRoutes()
    }

    fun listRouteWithDeleting(): List<Route> {
        return repository.loadRoutesWithDeleting()
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
            repository.saveRoute(route.copy(basicData = route.basicData.copy(timeStartWork = currentTimeInMillis)))
        } else {
            repository.saveRoute(route)
        }
    }

    fun isSynchronizedBasicData(basicId: String): Flow<ResultState<Unit>> {
        return repository.isSynchronizedBasicData(basicId)
    }

    fun setRemoteObjectIdBasicData(
        basicId: String,
        remoteObjectId: String
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

    fun getMinRest(route: Route, minTimeRest: Long): Long? {
        return route.shortRest(minTimeRest)
    }

    fun fullRest(route: Route, minTimeRest: Long): Long? {
        return route.fullRest(minTimeRest)
    }

    fun clearLocalRouteRepository(): Flow<ResultState<Unit>> {
        return repository.clearRepository()
    }
}