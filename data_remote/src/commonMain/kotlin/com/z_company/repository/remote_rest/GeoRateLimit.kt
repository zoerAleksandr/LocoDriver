package com.z_company.repository.remote_rest

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlin.time.Clock

/**
 * Пауза гео-запросов после ответа 429 (анонимный лимит по IP, SCREEN_SPECS §6.9).
 * Пока пауза идёт, клиент не бьёт в сервер: интерактивные экраны показывают «Слишком много запросов»
 * с «Повторить» после Retry-After, фоновые («АВТО», остановки) молча берут запасной вариант.
 */
object GeoRateLimit {
    /** Пауза, если сервер не прислал Retry-After. */
    const val DEFAULT_PAUSE_MS = 60_000L
    private const val MAX_PAUSE_MS = 3_600_000L

    private var untilMs: Long = 0L

    fun remainingMs(nowMs: Long = Clock.System.now().toEpochMilliseconds()): Long =
        (untilMs - nowMs).coerceAtLeast(0L)

    /** Если в цепочке причин есть 429 - запомнить паузу. true - лимит сработал. */
    fun recordIfRateLimited(chain: List<Throwable>, nowMs: Long = Clock.System.now().toEpochMilliseconds()): Boolean {
        val r = chain.filterIsInstance<ResponseException>()
            .firstOrNull { it.response.status == HttpStatusCode.TooManyRequests } ?: return false
        val pause = retryAfterMs(r) ?: DEFAULT_PAUSE_MS
        untilMs = maxOf(untilMs, nowMs + pause.coerceIn(1_000L, MAX_PAUSE_MS))
        return true
    }

    /** Retry-After в секундах -> мс; HTTP-дата и мусор -> null. */
    fun retryAfterMs(e: ResponseException): Long? =
        e.response.headers["Retry-After"]?.trim()?.toLongOrNull()?.takeIf { it >= 0 }?.times(1000)

    fun reset() { untilMs = 0L }
}
