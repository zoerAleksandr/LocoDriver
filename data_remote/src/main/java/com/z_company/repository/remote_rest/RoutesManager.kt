package com.z_company.repository.remote_rest

import android.util.Log
import androidx.lifecycle.application
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.repository.SecureDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

object RoutesManager : KoinComponent {
    private val remoteRestApi: RemoteRestApi by inject()

    fun saveRouteInRemote(
        route: Route,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        try {
            val response = remoteRestApi.saveRoute(
                token = bearerToken,
                data = route
            )
            if (response.isSuccessful) {
                emit(ResultState.Success(Unit))
            } else {
                val errorBody = response.errorBody()
                    ?.string() // Изменено: Добавил safe чтение errorBody как string.
                Log.d("zzz", "save route error: code=${response.code()}, body=$errorBody")
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения: ${response.code()} - $errorBody")))
            }
        } catch (e: Exception) {
            Log.d("zzz", "save route ${e.message}")
            emit(ResultState.Error(ErrorEntity(throwable = e))) // Другие ошибки
        }
    }.flowOn(Dispatchers.IO) // Добавлено: flowOn(Dispatchers.IO)
        .catch { e ->
            Log.d("zzz", "catch $e ${e.message}")
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }

    fun getRoutesFromRemote(bearerToken: String): Flow<ResultState<List<Route>>> = flow {
        emit(ResultState.Loading())
        try {
            val response = remoteRestApi.getRoutes(
                token = bearerToken,
            )
            val routes = response.body() ?: emptyList()
            if (response.isSuccessful) {
                emit(ResultState.Success(routes))
            } else {
                emit(ResultState.Error(ErrorEntity(message = response.code().toString())))
            }
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }
    }
}