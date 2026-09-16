package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Истёкший bearer-токен (401 «Invalid creadential» на любом запросе с
 * `Authorization`) должен превращаться в одно понятное сообщение, а не в сырой
 * `detail` по каждому маршруту в отчёте об ошибках синхронизации.
 */
class SessionExpiredMappingTest {

    private val expiredBody = """{"detail":"Invalid creadential"}"""

    @Test
    fun `401 с bearer-токеном — сессия истекла`() {
        assertTrue(NetworkErrorMapper.isSessionExpiredResponse(401, expiredBody, hadAuthorization = true))
    }

    @Test
    fun `401 без Authorization — это не наша сессия (неверный пароль на логине)`() {
        assertFalse(NetworkErrorMapper.isSessionExpiredResponse(401, expiredBody, hadAuthorization = false))
    }

    @Test
    fun `401 vk_token_invalid при привязке VK не разлогинивает`() {
        val body = """{"detail":"$VK_DETAIL_TOKEN_INVALID"}"""
        assertFalse(NetworkErrorMapper.isSessionExpiredResponse(401, body, hadAuthorization = true))
    }

    @Test
    fun `401 с пустым телом всё равно считается истёкшей сессией`() {
        assertTrue(NetworkErrorMapper.isSessionExpiredResponse(401, null, hadAuthorization = true))
    }

    @Test
    fun `другие коды не трогаем`() {
        assertFalse(NetworkErrorMapper.isSessionExpiredResponse(403, expiredBody, hadAuthorization = true))
        assertFalse(NetworkErrorMapper.isSessionExpiredResponse(422, expiredBody, hadAuthorization = true))
    }

    @Test
    fun `ошибка сохранения маршрута по 401 — понятный текст вместо detail`() {
        assertEquals(
            NetworkErrorMapper.SESSION_EXPIRED_MESSAGE,
            RoutesManager.parseServerError(401, expiredBody),
        )
    }

    @Test
    fun `ошибку шага с префиксом распознаём по маркеру`() {
        val stepError = "Ошибка сохранения UserSettings: ${NetworkErrorMapper.SESSION_EXPIRED_MESSAGE}"
        assertTrue(NetworkErrorMapper.isSessionExpiredMessage(stepError))
        assertFalse(NetworkErrorMapper.isSessionExpiredMessage("Сервер отклонил запрос (код 422)."))
        assertFalse(NetworkErrorMapper.isSessionExpiredMessage(null))
    }

    @Test
    fun `snackbar получает сообщение без префикса шага`() {
        val stepError = "Ошибка загрузки маршрутов: ${NetworkErrorMapper.SESSION_EXPIRED_MESSAGE}"
        assertEquals(NetworkErrorMapper.SESSION_EXPIRED_MESSAGE, NetworkErrorMapper.syncFailureMessage(stepError))
    }

    @Test
    fun `сессия истекла — не сетевая ошибка, экран Нет интернета не показываем`() {
        assertFalse(NetworkErrorMapper.isConnectivityMessage(NetworkErrorMapper.SESSION_EXPIRED_MESSAGE))
    }
}

/**
 * Первая ошибка «сессия истекла» завершает поток синхронизации: дальше все
 * запросы с тем же токеном получат 401, а upstream должен освободить мьютекс.
 */
class AbortOnSessionExpiredTest {

    /** Как в RouteDeltaTest: kotlinx-coroutines-test в проекте нет, поток не ждёт I/O. */
    private fun runSync(block: suspend () -> Unit) {
        var outcome: Result<Unit>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
        (outcome ?: error("Поток приостановился, чего фейковый upstream делать не должен")).getOrThrow()
    }

    @Test
    fun `поток обрывается на первой ошибке истёкшей сессии, upstream завершается`() = runSync {
        var upstreamFinished = false
        val upstream = flow {
            try {
                emit(ResultState.Success(1))
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${NetworkErrorMapper.SESSION_EXPIRED_MESSAGE}")))
                emit(ResultState.Success(2))
                emit(ResultState.Error(ErrorEntity(message = "[id] Маршрут: ${NetworkErrorMapper.SESSION_EXPIRED_MESSAGE}")))
            } finally {
                upstreamFinished = true
            }
        }

        val collected = upstream.abortOnSessionExpired().toList()

        assertEquals(2, collected.size)
        assertTrue(collected.last() is ResultState.Error)
        assertTrue(upstreamFinished)
    }

    @Test
    fun `обычные ошибки шага поток не обрывают`() = runSync {
        val upstream = flow {
            emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: Сервер отклонил запрос (код 422).")))
            emit(ResultState.Success(Unit))
        }
        assertEquals(2, upstream.abortOnSessionExpired().toList().size)
    }
}
