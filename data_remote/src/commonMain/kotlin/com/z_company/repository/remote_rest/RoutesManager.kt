package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Менеджер синхронизации маршрутов.
 * Шаг 2 KMP-миграции: object → class с инжекцией зависимостей через Koin.
 */
class RoutesManager(
    private val remoteRestApi: RemoteRestApi
) {

    fun saveRouteInRemote(
        route: Route,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        try {
            remoteRestApi.saveRoute(token = bearerToken, data = route)
            emit(ResultState.Success(Unit))
        } catch (e: ClientRequestException) {
            emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения: ${e.response.status.value}")))
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }
    }.flowOn(Dispatchers.Default)
        .catch { e ->
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }

    fun getRoutesFromRemote(bearerToken: String): Flow<ResultState<List<Route>>> = flow {
        emit(ResultState.Loading())
        try {
            val routes = remoteRestApi.getRoutes(token = bearerToken)
            emit(ResultState.Success(routes))
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }
    }
}
