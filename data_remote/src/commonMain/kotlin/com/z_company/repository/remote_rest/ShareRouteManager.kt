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
 * Одиночная ссылка формируется как `https://locodriver.ru/r/{id}`. Для нескольких
 * маршрутов первый share id остаётся в пути, остальные передаются в `?routes=`.
 * Deep link обрабатывается в Android `MainActivity` и SwiftUI `iOSApp`.
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
         * Собирает одну ссылку на несколько публичных маршрутов.
         * Серверные записи остаются отдельными, а в URL передаются их share id.
         */
        fun buildShareUrl(ids: List<String>): String {
            require(ids.isNotEmpty()) { "At least one share id is required" }
            val firstUrl = buildShareUrl(ids.first())
            if (ids.size == 1) return firstUrl
            return "$firstUrl?routes=${ids.drop(1).joinToString(",")}"
        }

        /**
         * Извлекает первый shareId из любого поддерживаемого формата ссылки.
         *  - `https://locodriver.ru/r/{id}`           — основной (App Links)
         *  - `https://locodriver.ru/share/{id}`       — на случай редиректа со старого пути
         *  - `locodriver://share/{id}`                — кастомная схема (fallback / iOS)
         *  - `locodriver://r/{id}`                    — на случай ручного формирования
         *
         * Для общей ссылки возвращает первый id, поэтому старые версии приложения
         * продолжают открывать первый маршрут. Если ссылка не распознана — null.
         */
        fun parseShareId(url: String): String? {
            return parseShareIds(url).firstOrNull()
        }

        /** Извлекает один или несколько share id из поддерживаемого формата ссылки. */
        fun parseShareIds(url: String): List<String> {
            if (url.isBlank()) return emptyList()
            // /r/   — основной формат: https://locodriver.ru/r/{id}
            // /share/ — кастомная схема: locodriver://share/{id} (содержит "/share/" из-за "://")
            val markers = listOf("/$SHARE_HTTPS_PATH/", "/$SHARE_HOST/")
            for (marker in markers) {
                val idx = url.indexOf(marker)
                if (idx < 0) continue
                val tail = url.substring(idx + marker.length)
                val pathIds = tail.substringBefore('?').substringBefore('#')
                    .replace("%2C", ",", ignoreCase = true)
                    .trim('/')
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val query = url.substringAfter('?', "").substringBefore('#')
                val queryIds = query.split('&')
                    .firstOrNull { it.substringBefore('=') == "routes" }
                    ?.substringAfter('=', "")
                    ?.replace("%2C", ",", ignoreCase = true)
                    ?.split(',')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty()
                val ids = pathIds + queryIds
                if (ids.isNotEmpty()) return ids
            }
            return emptyList()
        }
    }
}
