package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
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
            // TODO: nested catch (_: Exception) поглощает CancellationException
            // при отмене корутины во время чтения тела. Заменить на:
            //   try { ... } catch (e: CancellationException) { throw e } catch (Exception) { "" }
            val errorBody = try {
                e.response.bodyAsText()
            } catch (_: Exception) {
                ""
            }
            val errorMessage = RoutesManager.parseServerError(e.response.status.value, errorBody)
            emit(ResultState.Error(ErrorEntity(message = errorMessage, appError = e.toAppError())))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
        }
    }.flowOn(Dispatchers.Default)
        .catch { e ->
            emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
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
            // TODO: nested catch (_: Exception) поглощает CancellationException
            // при отмене корутины во время чтения тела. Заменить на:
            //   try { ... } catch (e: CancellationException) { throw e } catch (Exception) { "" }
            val errorBody = try {
                e.response.bodyAsText()
            } catch (_: Exception) {
                ""
            }
            val errorMessage = RoutesManager.parseServerError(e.response.status.value, errorBody)
            emit(ResultState.Error(ErrorEntity(message = errorMessage, appError = e.toAppError())))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
        }
    }.flowOn(Dispatchers.Default)
        .catch { e ->
            emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
        }

    companion object {
        /** Кастомная URI-схема (fallback и iOS-распространение без Apple Developer Program). */
        const val SHARE_SCHEME: String = "locodriver"
        /** Хост для кастомной схемы (`locodriver://share/{id}`). */
        const val SHARE_HOST: String = "share"

        /**
         * HTTPS-домен для App Links / Universal Links и для редирект-страницы,
         * раскатанной на Cloudflare Pages из репозитория `locodriver-site`.
         */
        const val SHARE_HTTPS_HOST: String = "locodriver.ru"
        /** Префикс пути на HTTPS-домене для shared-маршрутов. */
        const val SHARE_HTTPS_PATH: String = "r"

        /**
         * Собирает публичную ссылку. Используется HTTPS-вариант, потому что только он
         * кликабелен в Telegram/WhatsApp и поддерживает Android App Links.
         * Старый формат `locodriver://share/{id}` остаётся валидным для входящих deep-link-ов.
         */
        fun buildShareUrl(id: String): String =
            "https://$SHARE_HTTPS_HOST/$SHARE_HTTPS_PATH/$id"

        /**
         * Извлекает shareId из любого поддерживаемого формата ссылки:
         *  - `https://locodriver.ru/r/{id}`           — основной (App Links)
         *  - `https://locodriver.ru/share/{id}`       — на случай редиректа со старого пути
         *  - `locodriver://share/{id}`                — кастомная схема (fallback / iOS)
         *  - `locodriver://r/{id}`                    — на случай ручного формирования
         *
         * Возвращает null, если ни один маркер не распознан или id пустой.
         */
        fun parseShareId(url: String): String? {
            if (url.isBlank()) return null
            // /r/   — основной формат: https://locodriver.ru/r/{id}
            // /share/ — кастомная схема: locodriver://share/{id} (содержит "/share/" из-за "://")
            val markers = listOf("/$SHARE_HTTPS_PATH/", "/$SHARE_HOST/")
            for (marker in markers) {
                val idx = url.indexOf(marker)
                if (idx < 0) continue
                val tail = url.substring(idx + marker.length)
                val id = tail.substringBefore('?').substringBefore('#').trim('/').trim()
                if (id.isNotEmpty()) return id
            }
            return null
        }
    }
}
