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

/**
 * Менеджер публичных ссылок на маршруты.
 *
 * Через серверные эндпоинты:
 *   POST /v1/share/route       — создаёт публичную запись, возвращает короткий id
 *   GET  /v1/share/route/{id}  — возвращает Route по id (без авторизации)
 *
 * Ссылка формируется на клиенте в формате: `locodriver://share/{id}`.
 * Deep link обрабатывается в `MainActivity` (Android) и `iOSApp.swift` (iOS).
 */
class ShareRouteManager(
    private val remoteRestApi: RemoteRestApi
) {

    /**
     * Создаёт публичную ссылку на [route]. Требует авторизации.
     */
    fun createShareLink(
        route: Route,
        bearerToken: String
    ): Flow<ResultState<String>> = flow {
        emit(ResultState.Loading())
        try {
            val response = remoteRestApi.createSharedRoute(token = bearerToken, data = route)
            emit(ResultState.Success(buildShareUrl(response.id)))
        } catch (e: ClientRequestException) {
            val errorBody = try {
                e.response.bodyAsText()
            } catch (_: Exception) {
                ""
            }
            val errorMessage = RoutesManager.parseServerError(e.response.status.value, errorBody)
            emit(ResultState.Error(ErrorEntity(message = errorMessage)))
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }
    }.flowOn(Dispatchers.Default)
        .catch { e ->
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }

    /**
     * Получает маршрут по [shareId] (идентификатор из публичной ссылки).
     * Не требует авторизации.
     */
    fun getSharedRoute(shareId: String): Flow<ResultState<Route>> = flow {
        emit(ResultState.Loading())
        try {
            val route = remoteRestApi.getSharedRoute(shareId)
            emit(ResultState.Success(route))
        } catch (e: ClientRequestException) {
            val errorBody = try {
                e.response.bodyAsText()
            } catch (_: Exception) {
                ""
            }
            val errorMessage = RoutesManager.parseServerError(e.response.status.value, errorBody)
            emit(ResultState.Error(ErrorEntity(message = errorMessage)))
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }
    }.flowOn(Dispatchers.Default)
        .catch { e ->
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }

    companion object {
        /** Префикс кастомной URI-схемы. */
        const val SHARE_SCHEME: String = "locodriver"
        /** Хост пути. */
        const val SHARE_HOST: String = "share"

        /** Собирает полную ссылку вида `locodriver://share/{id}`. */
        fun buildShareUrl(id: String): String = "$SHARE_SCHEME://$SHARE_HOST/$id"

        /**
         * Извлекает shareId из ссылки формата `locodriver://share/{id}`
         * или `https://*/share/{id}` (для будущего варианта с Universal Links).
         * Возвращает null, если формат не распознан.
         */
        fun parseShareId(url: String): String? {
            if (url.isBlank()) return null
            val marker = "$SHARE_HOST/"
            val idx = url.indexOf(marker)
            if (idx < 0) return null
            val tail = url.substring(idx + marker.length)
            val id = tail.substringBefore('?').substringBefore('#').trim('/').trim()
            return id.ifEmpty { null }
        }
    }
}
