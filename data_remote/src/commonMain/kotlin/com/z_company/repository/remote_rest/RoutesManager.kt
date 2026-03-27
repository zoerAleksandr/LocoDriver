package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Менеджер синхронизации маршрутов.
 */
class RoutesManager(
    private val remoteRestApi: RemoteRestApi
) {

    /**
     * Результат сохранения маршрута на сервер.
     * warnings — список предупреждений (невалидные значения записаны как NULL).
     */
    data class SaveRouteResult(
        val warnings: List<String> = emptyList()
    )

    fun saveRouteInRemote(
        route: Route,
        bearerToken: String
    ): Flow<ResultState<SaveRouteResult>> = flow {
        emit(ResultState.Loading())
        try {
            val response = remoteRestApi.saveRoute(token = bearerToken, data = route)
            emit(ResultState.Success(SaveRouteResult(warnings = response.warnings)))
        } catch (e: ClientRequestException) {
            val errorBody = try { e.response.bodyAsText() } catch (_: Exception) { "" }
            val errorMessage = parseServerError(e.response.status.value, errorBody)
            emit(ResultState.Error(ErrorEntity(message = errorMessage)))
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }
    }.flowOn(Dispatchers.Default)
        .catch { e ->
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }

    fun deleteRouteInRemote(
        routeId: String,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        try {
            remoteRestApi.deleteRoute(token = bearerToken, routeId = routeId)
            emit(ResultState.Success(Unit))
        } catch (e: ClientRequestException) {
            val errorBody = try { e.response.bodyAsText() } catch (_: Exception) { "" }
            val errorMessage = parseServerError(e.response.status.value, errorBody)
            emit(ResultState.Error(ErrorEntity(message = errorMessage)))
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

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /**
         * Парсит тело ошибки сервера в читаемое сообщение.
         * 500: {"detail": "Маршрут сохранен частично. Ошибки: ..."}
         * 422: {"detail": [{"loc": [...], "msg": "...", "type": "..."}]}
         */
        fun parseServerError(statusCode: Int, body: String): String {
            if (body.isBlank()) return "Ошибка сервера: $statusCode"
            return try {
                val jsonElement = json.parseToJsonElement(body)
                val detail = jsonElement.jsonObject["detail"]
                when {
                    // 422: detail — массив ошибок валидации
                    detail != null && statusCode == 422 -> {
                        val errors = detail.jsonArray.map { err ->
                            val obj = err.jsonObject
                            val loc = obj["loc"]?.jsonArray
                                ?.drop(1) // убираем "body"
                                ?.joinToString(" -> ") { it.jsonPrimitive.content }
                                ?: "?"
                            val msg = obj["msg"]?.jsonPrimitive?.content ?: "ошибка"
                            "$loc: $msg"
                        }
                        "Ошибка валидации: ${errors.joinToString("; ")}"
                    }
                    // 500: detail — строка
                    detail != null -> {
                        detail.jsonPrimitive.content
                    }
                    else -> "Ошибка сервера: $statusCode"
                }
            } catch (_: Exception) {
                body.take(300)
            }
        }
    }
}
