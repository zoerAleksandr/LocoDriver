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
    // Публичный: используется и как база Ktor-клиента, и для склейки абсолютных
    // URL картинок фич обновления (см. RemoteAnnouncementRepository).
    const val BASE_URL = "http://87.228.110.32:8766/"
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
        KtorRemoteRestApi(createClient(BASE_URL))
    }

    val apiForSendEmail: ApiForSendEmail by lazy {
        KtorApiForSendEmail(createClient(BASE_URL_FOR_SEND_EMAIL))
    }

    private fun createClient(baseUrl: String): HttpClient = HttpClient(createHttpEngine()) {
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
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
        install(DefaultRequest) {
            url(baseUrl)
        }
    }
}
