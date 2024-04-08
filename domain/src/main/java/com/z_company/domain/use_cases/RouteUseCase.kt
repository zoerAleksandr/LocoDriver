package com.z_company.domain.use_cases

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.repositories.RemoteRouteRepository
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.util.moreThan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.z_company.domain.util.minus
import com.z_company.domain.util.div
import com.z_company.domain.util.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class RouteUseCase(
    private val repository: RouteRepository,
    private val remoteRepository: RemoteRouteRepository
) {
    fun listRoutes(): Flow<ResultState<List<Route>>> {
        return repository.loadRoutes()
    }

    fun routeDetails(routeId: String): Flow<ResultState<Route?>> {
        return repository.loadRoute(routeId)
    }

    fun removeRoute(route: Route): Flow<ResultState<Unit>> {
        return repository.remove(route)
    }

    fun saveRoute(route: Route): Flow<ResultState<Unit>> {
        remoteRepository.saveBasicData(route.basicData)

        return if (isRouteValid(route)) {
            repository.saveRoute(route)
        } else {
            flowOf(
                ResultState.Error(
                    ErrorEntity(IllegalStateException(), "-1", "Route is not valid.")
                )
            )
        }
    }

    private fun isRouteValid(route: Route): Boolean {
        // TODO("Not yet implemented")
        return true
    }

    fun isTimeWorkValid(route: Route): Boolean {
        val startTime = route.basicData.timeStartWork
        val endTime = route.basicData.timeEndWork

        return !startTime.moreThan(endTime)
    }

    fun getMinRest(route: Route, minTimeRest: Long?): Long?{
        return if (isTimeWorkValid(route)) {
            val startTime = route.basicData.timeStartWork
            val endTime = route.basicData.timeEndWork
            val timeResult = endTime - startTime
            val halfRest = timeResult / 2
            halfRest?.let { half ->
                if (half.moreThan(minTimeRest)) {
                    endTime + half
                } else {
                    endTime + minTimeRest
                }
            }
        } else {
            null
        }
    }

    fun fullRest(route: Route): Long?{
        val startTime = route.basicData.timeStartWork
        val endTime = route.basicData.timeEndWork
        val timeResult = endTime - startTime
        return if (isTimeWorkValid(route)) {
            endTime + timeResult
        } else {
            null
        }
    }
}