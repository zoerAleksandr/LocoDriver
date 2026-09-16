package com.z_company.repository.remote_rest

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Решение «пора ли продлевать токен» принимается по `iat` из payload JWT.
 * Подпись здесь не проверяется — это делает сервер; клиенту важно только не
 * ходить за новым токеном при каждом запуске и не пропустить старый формат.
 */
@OptIn(ExperimentalEncodingApi::class)
class SessionRefresherTest {

    private val day = 24L * 60 * 60 * 1000

    private fun jwt(payloadJson: String): String {
        val encoder = Base64.UrlSafe
        val header = encoder.encode("""{"alg":"HS256","typ":"JWT"}""".encodeToByteArray()).trimEnd('=')
        val payload = encoder.encode(payloadJson.encodeToByteArray()).trimEnd('=')
        return "$header.$payload.signature"
    }

    @Test
    fun `iat читается из payload без padding`() {
        assertEquals(1_700_000_000L, accessTokenIssuedAt(jwt("""{"sub":"u","iat":1700000000,"exp":1731536000}""")))
    }

    @Test
    fun `токен старого формата без iat — null`() {
        assertNull(accessTokenIssuedAt(jwt("""{"sub":"u","exp":1731536000}""")))
    }

    @Test
    fun `мусор вместо токена — null, а не исключение`() {
        assertNull(accessTokenIssuedAt("not-a-jwt"))
        assertNull(accessTokenIssuedAt("a.!!!.c"))
        assertNull(accessTokenIssuedAt(""))
    }

    @Test
    fun `свежий токен не обновляем`() {
        val issued = 1_700_000_000L
        val token = jwt("""{"sub":"u","iat":$issued}""")
        assertFalse(isRefreshDue(token, nowMillis = issued * 1000 + 6 * day))
    }

    @Test
    fun `токен старше недели обновляем`() {
        val issued = 1_700_000_000L
        val token = jwt("""{"sub":"u","iat":$issued}""")
        assertTrue(isRefreshDue(token, nowMillis = issued * 1000 + 7 * day))
    }

    @Test
    fun `токен без iat обновляем при первом же запуске`() {
        assertTrue(isRefreshDue(jwt("""{"sub":"u"}"""), nowMillis = 0L))
    }
}
