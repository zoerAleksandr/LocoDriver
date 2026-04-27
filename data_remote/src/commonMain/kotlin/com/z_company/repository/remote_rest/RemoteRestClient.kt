package com.z_company.repository.remote_rest

import co.touchlab.kermit.Logger
import com.z_company.domain.entities.serializers.DoubleAsStringSerializer
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

/**
 * Фабрика Ktor HttpClient.
 *
 * Делится на ТРИ инстанса:
 *  - [remoteRestApi]   — главный, с retry. Используется для бизнеса (sync,
 *                        routes, settings, share). Transient-ошибки сети
 *                        повторяются 3 раза с exponential backoff.
 *  - [authRestApi]     — auth-операции (login/register/forgot/profile-check/
 *                        email-update). БЕЗ retry: 4xx — ошибки пользователя
 *                        (неверный пароль, существующий email), не transient.
 *  - [apiForSendEmail] — отдельный домен (forgot password), без retry.
 */
object RemoteRestClient {
    private const val BASE_URL = "http://87.228.110.32:8766/"
    private const val BASE_URL_FOR_SEND_EMAIL = "http://locodrivers.freemyip.com/"

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
        KtorRemoteRestApi(createClient(BASE_URL, withRetry = true))
    }

    val authRestApi: RemoteRestApi by lazy {
        KtorRemoteRestApi(createClient(BASE_URL, withRetry = false))
    }

    val apiForSendEmail: ApiForSendEmail by lazy {
        KtorApiForSendEmail(createClient(BASE_URL_FOR_SEND_EMAIL, withRetry = false))
    }

    private fun createClient(baseUrl: String, withRetry: Boolean): HttpClient =
        HttpClient(createHttpEngine()) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(appJson)
            }
            install(Logging) {
                // INFO, не BODY — иначе JWT/PII (email, refresh-токены)
                // утекают в логи приложения и Sentry.
                level = LogLevel.INFO
            }
            install(HttpRedirect) {
                allowHttpsDowngrade = false
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            if (withRetry) {
                install(HttpRequestRetry) {
                    maxRetries = 3
                    exponentialDelay(base = 2.0)
                    retryOnExceptionIf { _, cause ->
                        val shouldRetry = cause is IOException ||
                            cause is HttpRequestTimeoutException ||
                            cause::class.simpleName == "DarwinHttpRequestException"
                        if (shouldRetry) {
                            Logger.withTag("Retry").i {
                                "Retrying: cause=${cause::class.simpleName}"
                            }
                        }
                        shouldRetry
                    }
                    modifyRequest {
                        Logger.withTag("Retry").i { "Retry attempt #$retryCount" }
                    }
                }
            }
            install(DefaultRequest) {
                url(baseUrl)
                // Connection: close — защита от stale keep-alive (NSURLError -1005).
                // Trade-off: каждый запрос открывает новое TCP-соединение
                // (50-200ms overhead). Если sync станет медленным после релиза —
                // рассмотреть удаление и переход на per-endpoint
                // Connection: keep-alive.
                headers.append(HttpHeaders.Connection, "close")
            }
        }
}
