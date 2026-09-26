package com.z_company.core

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

/**
 * Сбои транспорта (нет сети, DNS, таймаут, обрыв TLS) — не баги приложения:
 * их уже обрабатывает NetworkErrorMapper и показывает пользователю «нет
 * соединения». В Sentry они давали 87% всех событий проекта (1099 из 1260 за
 * 20–26.09.2026) и выбирали квоту. Фильтруем централизованно в beforeSend, а
 * не по местам вызова: sendToSentry зовётся из десятков мест.
 */
private val TRANSPORT_EXCEPTION_TYPES = setOf(
    "UnknownHostException", "GaiException", "ErrnoException",
    "ConnectException", "ConnectTimeoutException", "NoRouteToHostException",
    "SocketException", "SocketTimeoutException", "InterruptedIOException",
    "HttpRequestTimeoutException",
    "SSLException", "SSLHandshakeException", "SSLPeerUnverifiedException",
)

/** Текстовые маркеры для captureMessage: у сообщений нет типа исключения. */
private val TRANSPORT_MESSAGE_MARKERS = listOf(
    "unable to resolve host", "failed to connect", "connection abort",
    "connection reset", "timed out", "timeout has expired",
    "network is unreachable", "unexpected end of stream",
)

private fun isTransportNoise(event: io.sentry.kotlin.multiplatform.SentryEvent): Boolean {
    val byException = event.exceptions?.any { exception ->
        exception.type?.substringAfterLast('.') in TRANSPORT_EXCEPTION_TYPES
    } == true
    if (byException) return true
    val text = event.message?.formatted ?: event.message?.message ?: return false
    val lower = text.lowercase()
    return TRANSPORT_MESSAGE_MARKERS.any { lower.contains(it) }
}

internal actual fun platformInitSentry(dsn: String, release: String?, environment: String?) {
    Sentry.init { options ->
        options.dsn = dsn
        release?.let { options.release = it }
        environment?.let { options.environment = it }
        options.beforeSend = { event ->
            val isCloseSystemDialogs = event.exceptions?.any { exception ->
                exception.type?.contains("SecurityException") == true &&
                    exception.value?.contains("CLOSE_SYSTEM_DIALOGS") == true
            } == true
            val isCancellation = event.exceptions?.any { exception ->
                exception.type?.contains("CancellationException") == true
            } == true
            if (isCloseSystemDialogs || isCancellation || isTransportNoise(event)) null else event
        }
    }
}

internal actual fun platformSendToSentry(throwable: Throwable, operation: String) {
    Sentry.addBreadcrumb(Breadcrumb.info(operation))
    Sentry.captureException(throwable)
}

internal actual fun platformSendMessageToSentry(message: String) {
    Sentry.captureMessage(message)
}
