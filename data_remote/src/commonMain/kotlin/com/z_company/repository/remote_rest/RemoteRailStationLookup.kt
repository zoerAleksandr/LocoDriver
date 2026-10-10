package com.z_company.repository.remote_rest

import com.z_company.domain.util.RailLookupResult
import com.z_company.domain.util.RailStation
import com.z_company.domain.util.RailStationLookup
import com.z_company.domain.util.RailCandidate
import com.z_company.repository.remote_rest.response.NearestStationCandidate
import com.z_company.repository.remote_rest.response.NearestStationRequest
import com.z_company.repository.remote_rest.response.NearestStationResponse
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * `POST /v1/service_phase/stations/nearest` с `alongRail=true`: ближайшая станция по рельсам.
 * Координата уходит только в этом запросе и не логируется. Токен необязателен. Любая ошибка (нет сети, 503, 429 - пауза по Retry-After) - [RailLookupResult.Unavailable].
 */
class RemoteRailStationLookup(
    private val api: RemoteRestApi,
    private val bearerToken: suspend () -> String?,
    private val timeoutMs: Long = 5_000,
) : RailStationLookup {

    override suspend fun nearestAlongRail(latitude: Double, longitude: Double, radiusM: Double, candidates: List<RailCandidate>): RailLookupResult = try {
        // Без аккаунта - запрос без Authorization (лимит по IP, SCREEN_SPECS §6.9).
        val token = bearerToken()?.takeIf { it.isNotBlank() }
        // Лимит 429: молча уходим на запасной вариант (справочник по прямой), пока не истечёт Retry-After.
        val paused = GeoRateLimit.remainingMs()
        if (paused > 0) RailLookupResult.Unavailable(paused) else withTimeout(timeoutMs) {
            api.nearestStation(
                token?.let { "Bearer $it" },
                NearestStationRequest(
                    latitude, longitude, radiusM, alongRail = true,
                    candidates = candidates.takeIf { it.isNotEmpty() }
                        ?.map { NearestStationCandidate(it.key, it.name, it.latitude, it.longitude) },
                ),
            )
        }.toRailResult()
    } catch (e: TimeoutCancellationException) {
        RailLookupResult.Unavailable()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        GeoRateLimit.recordIfRateLimited(listOf(e))
        RailLookupResult.Unavailable(GeoRateLimit.retryAfterMs(e))
    } catch (e: Exception) {
        RailLookupResult.Unavailable()
    }

    companion object {
        fun NearestStationResponse.toRailResult(): RailLookupResult {
            val st = station ?: return RailLookupResult.NoStation(reason)
            return RailLookupResult.Found(
                RailStation(
                    st.osmId, st.name, st.kind, st.esr, st.latitude, st.longitude, distanceAlongM, offsetM,
                    candidateKey = candidateKey.takeIf { source == "candidate" },
                ),
            )
        }
    }
}
