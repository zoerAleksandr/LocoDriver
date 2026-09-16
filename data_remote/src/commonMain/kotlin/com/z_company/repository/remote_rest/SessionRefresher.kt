package com.z_company.repository.remote_rest

import com.z_company.repository.SecureTokenStorage
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock

/**
 * Продление сессии без повторного входа.
 *
 * Access-токен живёт год (сервер с 2026-09: 365 дней + грейс 365 после `exp`)
 * и выдаётся только при входе. Чтобы активный пользователь никогда не
 * упирался в срок, при старте приложения токен старше [REFRESH_AFTER_MILLIS]
 * меняется на свежий через `POST /v1/auth/refresh`. Токены без `iat`
 * (выданы до 2026-09) обновляются при первом же запуске.
 *
 * Ошибки не поднимаются: нет сети — попробуем в следующий раз, токен ещё
 * жив. 401 на refresh означает, что сессию уже не продлить — Ktor-клиент
 * сам шлёт сигнал в [SessionExpiredNotifier], разлогин делает приложение.
 */
class SessionRefresher(
    private val remoteRestApi: RemoteRestApi,
    private val secureTokenStorage: SecureTokenStorage,
) {
    sealed interface Outcome {
        data object NoSession : Outcome
        data object NotDue : Outcome
        data object Refreshed : Outcome
        data class Failed(val cause: Throwable) : Outcome
    }

    suspend fun refreshIfDue(
        nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): Outcome {
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
        if (token.isNullOrBlank()) return Outcome.NoSession
        if (!isRefreshDue(token, nowMillis)) return Outcome.NotDue
        return try {
            val fresh = remoteRestApi.refreshToken("Bearer $token").accessToken
            if (fresh.isBlank()) return Outcome.Failed(IllegalStateException("empty access_token"))
            // Синхронизация, стартовавшая параллельно со старым токеном, доработает:
            // старый токен сервер принимает ещё год.
            secureTokenStorage.saveAuthToken(fresh)
            Outcome.Refreshed
        } catch (e: Exception) {
            Outcome.Failed(e)
        }
    }

    companion object {
        /** Раз в неделю: заметно реже, чем открывают приложение, и много короче срока токена. */
        const val REFRESH_AFTER_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}

/** Пора ли менять токен: нет `iat` (старый формат) или выдан давнее [refreshAfterMillis]. */
internal fun isRefreshDue(
    token: String,
    nowMillis: Long,
    refreshAfterMillis: Long = SessionRefresher.REFRESH_AFTER_MILLIS,
): Boolean {
    val issuedAtSeconds = accessTokenIssuedAt(token) ?: return true
    return nowMillis - issuedAtSeconds * 1000 >= refreshAfterMillis
}

/**
 * `iat` (секунды Unix) из payload JWT без проверки подписи — подпись
 * проверяет сервер, здесь только решаем, когда к нему идти. Любой мусор
 * вместо токена даёт null, что трактуется как «обновить».
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun accessTokenIssuedAt(token: String): Long? {
    val payload = token.split('.').getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
    return try {
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val json = Base64.UrlSafe.decode(padded).decodeToString()
        Json.parseToJsonElement(json).jsonObject["iat"]?.jsonPrimitive?.longOrNull
    } catch (_: Exception) {
        null
    }
}
