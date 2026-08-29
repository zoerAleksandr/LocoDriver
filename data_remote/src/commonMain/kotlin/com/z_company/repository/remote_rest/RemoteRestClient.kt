package com.z_company.repository.remote_rest

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import com.z_company.domain.entities.serializers.DoubleAsStringSerializer

/**
 * Фабрика Ktor HttpClient.
 * Заменяет Retrofit + OkHttp + Gson.
 * Движок HTTP задаётся через expect/actual (createHttpEngine()).
 */
object RemoteRestClient {
    const val PROD_BASE_URL = "http://87.228.110.32:8766/"
    const val PROD_BASE_URL_FOR_SEND_EMAIL = "http://locodrivers.freemyip.com/"

    /**
     * Публичный: используется и как база Ktor-клиента, и для склейки абсолютных
     * URL картинок фич обновления (см. RemoteAnnouncementRepository).
     *
     * По умолчанию — прод. Debug-сборка Android подменяет адрес на локальный
     * бэкенд в StartApp.onCreate() (см. [useBaseUrl]); в release подмены нет.
     */
    var BASE_URL: String = PROD_BASE_URL
        private set

    private var BASE_URL_FOR_SEND_EMAIL: String = PROD_BASE_URL_FOR_SEND_EMAIL

    /**
     * Переключает клиент на другой бэкенд — нужно, чтобы проверять сборку
     * против локального сервера, не трогая прод. Вызывать до первого сетевого
     * запроса (Application.onCreate); адрес читается на каждом запросе, так что
     * уже созданные клиенты подхватят его тоже.
     *
     * @param apiUrl база API. Пустая строка — оставить прод.
     * @param emailApiUrl база для путей `v1/page/` (письма, сброс пароля). По
     *   умолчанию тот же сервер: локальный бэкенд обслуживает и эти пути, а
     *   слать письма прод-пользователям из debug-сборки не нужно.
     */
    fun useBaseUrl(apiUrl: String, emailApiUrl: String = apiUrl) {
        if (apiUrl.isBlank()) return
        BASE_URL = apiUrl.withTrailingSlash()
        BASE_URL_FOR_SEND_EMAIL = emailApiUrl.ifBlank { apiUrl }.withTrailingSlash()
    }

    private fun String.withTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"

    val appJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
        serializersModule = SerializersModule {
            contextual(DoubleAsStringSerializer)
        }
    }

    val remoteRestApi: RemoteRestApi by lazy {
        KtorRemoteRestApi(createClient { BASE_URL })
    }

    val apiForSendEmail: ApiForSendEmail by lazy {
        KtorApiForSendEmail(createClient { BASE_URL_FOR_SEND_EMAIL })
    }

    private fun createClient(baseUrl: () -> String): HttpClient = HttpClient(createHttpEngine()) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(appJson)
        }
        install(Logging) {
            level = LogLevel.BODY
        }
        install(HttpRedirect) {
            allowHttpsDowngrade = false
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 25_000
            requestTimeoutMillis = 25_000
            socketTimeoutMillis = 25_000
        }
        install(DefaultRequest) {
            url(baseUrl())
        }
    }
}
