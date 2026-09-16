package com.z_company.repository.remote_rest

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Единая точка перевода сетевого исключения в понятное пользователю сообщение.
 *
 * Зачем: раньше любая ошибка выгрузки маскировалась под «Нет интернета». Причины
 * было две:
 *  1. Менеджеры отдавали [com.z_company.core.ErrorEntity] только с `throwable`,
 *     без `message`, а потребители подставляли фолбэк «Нет соединения» —
 *     и он же ловился классификатором как «нет сети».
 *  2. Классификация шла по подстрокам, а не по типу исключения.
 *
 * Теперь ответ сервера с кодом ошибки (валидация 4xx, сбой 5xx) НЕ считается
 * отсутствием интернета — соединение дошло, просто сервер данные не принял.
 * «Нет интернета» показываем только для реальных транспортных сбоев.
 */
object NetworkErrorMapper {

    /**
     * true — это реальный сбой транспорта (соединение не дошло до сервера),
     * а не ответ сервера с кодом ошибки.
     */
    fun isConnectivityError(throwable: Throwable?): Boolean = when (throwable) {
        // Сервер ответил статусом (4xx/5xx) — соединение есть, это не «нет сети».
        is ResponseException -> false
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException -> true
        is IOException -> true
        null -> false
        // Ошибки сериализации, маппинга и локальной БД не означают отсутствие сети.
        else -> false
    }

    /** Понятное пользователю сообщение по исключению. */
    fun humanMessage(throwable: Throwable?): String = when (throwable) {
        is ClientRequestException -> clientErrorMessage(throwable)
        is ServerResponseException ->
            "Сервер временно недоступен (код ${throwable.response.status.value}). Попробуйте позже."
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException -> TIMEOUT_MESSAGE
        is SerializationException ->
            "Ошибка обработки данных сервера: ${safeTechnicalMessage(throwable)}"
        is IOException -> NO_CONNECTION_MESSAGE
        null -> NO_CONNECTION_MESSAGE
        else -> "Ошибка синхронизации: ${safeTechnicalMessage(throwable)}"
    }

    /**
     * Похоже ли готовое сообщение на транспортную ошибку (нет сети), а не на
     * осмысленный ответ сервера. Используется UI, чтобы решить, показывать ли
     * экран «Нет интернета» вместо детальной ошибки шага.
     */
    fun isConnectivityMessage(message: String?): Boolean {
        val m = message?.lowercase() ?: return false
        return CONNECTIVITY_MARKERS.any { m.contains(it) }
    }

    private fun clientErrorMessage(e: ClientRequestException): String {
        val code = e.response.status.value
        val fields = extractRejectedFields(e.message)
        return when {
            // Ktor кладёт тело ответа в текст исключения — по нему отличаем
            // просроченный bearer-токен от 401 «VK не подтвердил токен».
            isSessionExpiredResponse(code, e.message, hadAuthorization = true) ->
                SESSION_EXPIRED_MESSAGE
            code == 422 && fields.isNotEmpty() ->
                "Сервер отклонил данные (код 422). Не поддерживаются поля: " +
                    "${fields.joinToString(", ")}. Обновите приложение или сервер."
            code == 422 ->
                "Сервер отклонил данные (код 422): несовместимая версия сервера — " +
                    "часть отправленных полей ещё не поддерживается."
            else -> "Сервер отклонил запрос (код $code)."
        }
    }

    /**
     * Лучшее-усилие извлечение имён полей из тела FastAPI-ошибки валидации,
     * которое Ktor кладёт в текст исключения (`... Text: "{...}"`). Ищем пути
     * вида ["body","<field>"] — с учётом того, что кавычки могут быть
     * экранированы. Никогда не бросает; при неудаче возвращает пустой список.
     */
    private fun extractRejectedFields(raw: String?): List<String> {
        val text = raw ?: return emptyList()
        return REJECTED_FIELD_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .filter { it != "body" }
            .distinct()
            .toList()
    }

    /**
     * Бэкенд отвечает 401 на любой запрос с просроченным или отозванным
     * bearer-токеном (`get_current_user` → «Invalid creadential»). Токен живёт
     * 180 дней и не продлевается, так что это штатный сценарий для каждого,
     * кто давно не перелогинивался. Показывать сырой `detail` нельзя —
     * пользователь должен понять, что делать.
     */
    const val SESSION_EXPIRED_MESSAGE = "Сессия истекла. Войдите в аккаунт заново."

    /**
     * Сессия истекла? 401 на запросе с bearer-токеном — да, кроме VK-привязки:
     * там 401 `vk_token_invalid` означает, что VK не подтвердил *свой* токен,
     * а наша сессия жива. [detailOrBody] — `detail` из тела ответа либо всё
     * тело/текст исключения, где этот `detail` содержится.
     */
    fun isSessionExpiredResponse(
        statusCode: Int,
        detailOrBody: String?,
        hadAuthorization: Boolean,
    ): Boolean = statusCode == 401 &&
        hadAuthorization &&
        detailOrBody?.contains(VK_DETAIL_TOKEN_INVALID) != true

    /**
     * Готовое сообщение — про истёкшую сессию? Ошибки шагов синхронизации
     * приходят с префиксом («Ошибка сохранения UserSettings: …»), поэтому
     * ищем маркер, а не сравниваем строки целиком.
     */
    fun isSessionExpiredMessage(message: String?): Boolean =
        message?.contains(SESSION_EXPIRED_MARKER, ignoreCase = true) == true

    private const val SESSION_EXPIRED_MARKER = "Сессия истекла"

    const val NO_CONNECTION_MESSAGE =
        "Нет соединения с сервером. Проверьте интернет и попробуйте снова."
    const val TIMEOUT_MESSAGE =
        "Превышено время ожидания ответа сервера. Проверьте соединение и попробуйте снова."
    /**
     * Без числа секунд: раньше здесь было зашито «за 25 секунд», и при
     * изменении [SyncManager.SYNC_OPERATION_TIMEOUT_MILLIS] текст начинал врать
     * пользователю. Формулировка намеренно не содержит маркеров из
     * [CONNECTIVITY_MARKERS] — иначе UI подменит детальную ошибку шага
     * экраном «Нет интернета».
     */
    const val SYNC_TIMEOUT_MESSAGE =
        "Синхронизация не выполнена: сервер не ответил вовремя. Проверьте соединение и попробуйте снова."

    fun syncFailureMessage(message: String?, throwable: Throwable? = null): String {
        val reason = message?.trim()?.takeIf { it.isNotEmpty() }
            ?: humanMessage(throwable)
        if (reason.startsWith("Синхронизация не выполнена", ignoreCase = true)) return reason
        // Без префикса шага: в snackbar важно только «что делать».
        if (isSessionExpiredMessage(reason)) return SESSION_EXPIRED_MESSAGE
        val conciseReason = reason.lineSequence().firstOrNull().orEmpty().take(240)
        return conciseReason
    }

    private fun safeTechnicalMessage(throwable: Throwable): String {
        val type = throwable::class.simpleName ?: "неизвестная ошибка"
        val details = throwable.message
            ?.lineSequence()
            ?.firstOrNull()
            ?.take(240)
            ?.trim()
            .orEmpty()
        return if (details.isBlank()) type else "$type: $details"
    }

    private val REJECTED_FIELD_REGEX =
        Regex("""body\\?"\s*,\s*\\?"([A-Za-z_][A-Za-z0-9_]*)""")

    private val CONNECTIVITY_MARKERS = listOf(
        "нет соединения", "превышено время ожидания", "проверьте интернет",
        "проверьте подключение", "нет интернета",
        "unable to resolve", "connection refused", "econnrefused",
        "connection reset", "failed to connect", "network is unreachable",
        "unexpected end of stream", "timeout", "timed out", "software caused",
        "stream was reset", "handshake",
        "unknownhostexception", "connectexception", "sockettimeoutexception"
    )
}
