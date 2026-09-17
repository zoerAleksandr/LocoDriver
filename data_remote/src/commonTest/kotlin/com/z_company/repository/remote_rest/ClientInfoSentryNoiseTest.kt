package com.z_company.repository.remote_rest

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `POST /v1/client-info` в начале синхронизации не должен слать в Sentry
 * штатные сбои: нет сети и истёкшую сессию (за неделю 10–16.09.2026 это
 * дало ~125 событий — треть проекта). Неожиданные ошибки — по-прежнему в Sentry.
 */
class ClientInfoSentryNoiseTest {

    @Test
    fun `нет сети — не шлём в Sentry`() {
        assertTrue(isExpectedClientInfoFailure(IOException("Unable to resolve host \"api.locodriver.ru\"")))
        assertTrue(isExpectedClientInfoFailure(ConnectTimeoutException("connect timeout")))
        assertTrue(isExpectedClientInfoFailure(SocketTimeoutException("read timeout")))
    }

    @Test
    fun `ошибки данных и логики — по-прежнему в Sentry`() {
        assertFalse(isExpectedClientInfoFailure(SerializationException("bad json")))
        assertFalse(isExpectedClientInfoFailure(IllegalStateException("unexpected")))
    }
}
