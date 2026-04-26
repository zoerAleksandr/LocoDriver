package com.z_company.repository.remote_rest

import co.touchlab.kermit.Logger
import com.z_company.core.AppError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

private const val TAG = "Network"

/**
 * Маппит произвольный [Throwable] в [AppError] для UI.
 *
 * Безопасность: НЕ логирует URL/тело/заголовки — только класс
 * исключения и (если получится извлечь) NSURL error code.
 *
 * NB: [CancellationException] пробрасывается как есть — это нормальный
 * отказ корутины (юзер ушёл с экрана), не показываем в UI.
 */
fun Throwable.toAppError(): AppError {
    if (this is CancellationException) throw this

    val mapped = doMap(this)

    // Шаг 4.0 диагностика: класс исключения В ЛЮБОМ СЛУЧАЕ — независимо
    // от того, сработал retry или нет. Помогает увидеть фактический
    // Ktor Darwin тип на iOS (например `DarwinHttpRequestException`),
    // чтобы починить `retryOnExceptionIf` condition если -1005 не
    // наследник `IOException`.
    Logger.withTag(TAG).i {
        "exception class=${this::class.simpleName} " +
            "qualifiedName=${this::class.qualifiedName?.take(80)} " +
            "message=${message?.take(150)}"
    }

    val nsurlCode = NSURL_CODE_REGEX.find(message ?: "")?.groupValues?.getOrNull(1)
    val logMessage: () -> String = {
        buildString {
            append("→ ${mapped::class.simpleName}")
            append(" cause=${this@toAppError::class.simpleName}")
            if (nsurlCode != null) append(" nsurl=$nsurlCode")
        }
    }
    val logger = Logger.withTag(TAG)
    when (mapped) {
        // Ожидаемые offline-сценарии — info, не засоряем .e().
        AppError.NoInternet, AppError.Timeout -> logger.i { logMessage() }
        // Серверные/auth проблемы — warning.
        AppError.ServerError, AppError.Unauthorized -> logger.w { logMessage() }
        // Truly unexpected — error.
        is AppError.Unknown -> logger.e { logMessage() }
    }
    return mapped
}

private fun doMap(t: Throwable): AppError = when {
    // Ktor HTTP errors — статус известен.
    t is ClientRequestException -> when (t.response.status.value) {
        401 -> AppError.Unauthorized
        in 400..499 -> AppError.ServerError
        else -> AppError.Unknown(t)
    }
    t is ServerResponseException -> AppError.ServerError
    t is RedirectResponseException -> {
        Logger.withTag(TAG).w {
            "Unexpected redirect ${t.response.status.value}"
        }
        AppError.ServerError
    }

    // Timeout.
    t is HttpRequestTimeoutException ||
    t is SocketTimeoutException ||
    t is ConnectTimeoutException -> AppError.Timeout

    // iOS Darwin engine: NSError завёрнут в `DarwinHttpRequestException`.
    // Этот класс НЕ наследник `IOException` (подтверждено в Шаге 4.0
    // диагностикой), поэтому проверяем по имени до общей IOException-ветки.
    t::class.simpleName == "DarwinHttpRequestException" ->
        classifyByMessage(t.message ?: "")

    // Сеть на JVM/Android: kotlinx.io.IOException.
    t is IOException -> classifyByMessage(t.message ?: "")

    // Резерв: NSError проскочил в каком-то другом обёрточном типе — пробуем по строке.
    NSURL_DOMAIN_REGEX.containsMatchIn(t.message ?: "") ->
        classifyByMessage(t.message ?: "")

    else -> AppError.Unknown(t)
}

private fun classifyByMessage(msg: String): AppError {
    val code = NSURL_CODE_REGEX.find(msg)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return when (code) {
        -1001 -> AppError.Timeout                      // NSURLErrorTimedOut
        -1005 -> AppError.NoInternet                   // NetworkConnectionLost
        -1009 -> AppError.NoInternet                   // NotConnectedToInternet
        -1003 -> AppError.NoInternet                   // CannotFindHost
        -1004 -> AppError.NoInternet                   // CannotConnectToHost
        -1018 -> AppError.NoInternet                   // InternationalRoamingOff
        -1019 -> AppError.NoInternet                   // CallIsActive
        -1020 -> AppError.NoInternet                   // DataNotAllowed
        else -> when {
            msg.contains("timed out", ignoreCase = true) -> AppError.Timeout
            else -> AppError.NoInternet
        }
    }
}

private val NSURL_CODE_REGEX = Regex("""Code=(-?\d+)""")

// TODO: hard-coded строка из NSError description.
// Альтернатива: проверять `t::class.simpleName == "DarwinHttpRequestException"`
// (если такой тип есть в Ktor 3.0.3 — нужно проверить при компиляции).
private val NSURL_DOMAIN_REGEX = Regex("""NSURLError""")
