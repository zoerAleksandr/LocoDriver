package com.z_company.core

/**
 * Высокоуровневая модель ошибок для UI.
 *
 * `userMessage` — короткое сообщение для пользователя (показывается в alert).
 * `technicalDetails` — только для логов, НИКОГДА в UI.
 *
 * Маппинг из [Throwable] — в `data_remote/.../ErrorMapper.kt` (`Throwable.toAppError()`).
 */
sealed class AppError(
    val userMessage: String,
    val technicalDetails: String? = null,
) {
    /** -1009/-1005/-1003/-1004 NSURLError, IOException без internet. */
    object NoInternet : AppError("Нет соединения с интернетом")

    /** -1001 NSURLErrorTimedOut, HttpRequestTimeoutException, SocketTimeoutException. */
    object Timeout : AppError("Сервер не отвечает. Попробуйте снова.")

    /** 5xx, либо 4xx кроме 401. */
    object ServerError : AppError("Ошибка сервера. Попробуйте позже.")

    /** 401 — нужен повторный логин. */
    object Unauthorized : AppError("Нужна повторная авторизация")

    /** Всё остальное. cause — только для логов. */
    data class Unknown(val cause: Throwable) : AppError(
        userMessage = "Что-то пошло не так",
        technicalDetails = cause::class.simpleName,
    )

    /** UI может показать кнопку "Повторить" для transient-ошибок. */
    val canRetry: Boolean
        get() = this is NoInternet || this is Timeout || this is ServerError
}
