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

    const val NO_CONNECTION_MESSAGE =
        "Нет соединения с сервером. Проверьте интернет и попробуйте снова."
    const val TIMEOUT_MESSAGE =
        "Превышено время ожидания ответа сервера. Проверьте соединение и попробуйте снова."

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
