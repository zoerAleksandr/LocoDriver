package com.z_company.repository.remote_rest

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpMethod
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
    /**
     * HTTPS через Caddy на том же сервере: api.locodriver.ru (Let's Encrypt)
     * проксирует на 127.0.0.1:8766, то есть тот же backend, что и раньше.
     *
     * До этого клиент ходил на http://87.228.110.32:8766/ открытым текстом —
     * access-токен и все данные рейсов шли по сети без шифрования. Прямой порт
     * 8766 остаётся открытым, пока не обновятся установленные сборки.
     */
    const val PROD_BASE_URL = "https://api.locodriver.ru/"
    /**
     * Почтовые endpoint'ы обслуживает тот же backend через основной HTTPS-домен.
     * Отдельный HTTP DDNS-домен здесь не используем: его блокировка DNS/HTTP-
     * фильтрами на устройстве раньше выглядела для пользователя как отсутствие сети.
     */
    const val PROD_BASE_URL_FOR_SEND_EMAIL = PROD_BASE_URL

    /**
     * Соединение либо устанавливается быстро, либо не устанавливается вовсе —
     * долго ждать здесь нечего, это только съедает общий бюджет синхронизации.
     */
    const val CONNECT_TIMEOUT_MILLIS = 15_000L

    /**
     * Чтение ответа. Прежние 25 с не выдерживала полная выгрузка маршрутов на
     * мобильной связи — именно этот таймаут пользователи видели как «сервер не
     * ответил за 25 секунд». Сжатие на сервере ужало ответ примерно в 9 раз, но
     * в кабине движущегося локомотива канал проседает, и запасу времени взяться
     * неоткуда — даём запросу минуту.
     */
    const val REQUEST_TIMEOUT_MILLIS = 60_000L
    const val SOCKET_TIMEOUT_MILLIS = 60_000L

    /** Один повтор: осечка на секунду не должна показывать пользователю ошибку. */
    const val TRANSIENT_RETRY_COUNT = 1

    /** Пауза перед повтором — дать связи восстановиться, а не бить сразу. */
    const val TRANSIENT_RETRY_DELAY_MILLIS = 2_000L

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

    /**
     * Повторяем только то, что безопасно повторить вслепую: GET, PUT и DELETE
     * идемпотентны по определению HTTP.
     *
     * POST исключён намеренно. Среди них есть неидемпотентные (`v1/auth/create` —
     * повтор может создать второй аккаунт или дать ложное «email занят»), а
     * разбирать POST-ы по списку путей — это правило, которое молча протухнет при
     * добавлении нового эндпоинта. Выгрузка на сервер от этого не страдает:
     * неотправленные изменения остаются помеченными и уедут следующей
     * синхронизацией.
     */
    private fun isRetryableMethod(method: HttpMethod): Boolean =
        method == HttpMethod.Get || method == HttpMethod.Put || method == HttpMethod.Delete

    /**
     * Повторяем только транспортные сбои — соединение не дошло до сервера.
     * Ответ сервера с кодом ошибки повторять бессмысленно: 4xx повтор не
     * исправит, а 5xx у нас означает отвергнутые данные, а не временный сбой.
     *
     * [HttpRequestTimeoutException] исключён отдельно: он означает, что весь
     * бюджет запроса ([REQUEST_TIMEOUT_MILLIS]) уже израсходован. Повтор в этом
     * случае почти никогда не успевает, зато удваивает ожидание и съедает общий
     * дедлайн синхронизации.
     */
    private fun isTransientFailure(cause: Throwable): Boolean =
        cause !is HttpRequestTimeoutException && NetworkErrorMapper.isConnectivityError(cause)

    private fun createClient(baseUrl: () -> String): HttpClient = HttpClient(createHttpEngine()) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(appJson)
        }
        install(Logging) {
            // Полные JSON-тела маршрутов бывают большими. Их форматирование и печать
            // отнимают CPU/память во время синхронизации и особенно заметны на слабой
            // мобильной сети. Заодно BODY мог содержать пользовательские данные.
            level = LogLevel.NONE
        }
        install(HttpRedirect) {
            allowHttpsDowngrade = false
        }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
        }
        install(HttpRequestRetry) {
            retryOnExceptionIf(TRANSIENT_RETRY_COUNT) { request, cause ->
                isRetryableMethod(request.method) && isTransientFailure(cause)
            }
            constantDelay(TRANSIENT_RETRY_DELAY_MILLIS, 0L, false)
        }
        install(DefaultRequest) {
            url(baseUrl())
        }
    }
}
