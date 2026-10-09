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
 * Координата уходит только в этом запросе и не логируется. Любая ошибка (нет сети, 503, нет токена) - [RailLookupResult.Unavailable].
 */
class RemoteRailStationLookup(
    private val api: RemoteRestApi,
    private val bearerToken: suspend () -> String?,
    private val timeoutMs: Long = 5_000,
) : RailStationLookup {

    override suspend fun nearestAlongRail(latitude: Double, longitude: Double, radiusM: Double, candidates: List<RailCandidate>): RailLookupResult = try {
        val token = bearerToken()?.takeIf { it.isNotBlank() }
        if (token == null) {
            RailLookupResult.Unavailable()
        } else {
            withTimeout(timeoutMs) {
                api.nearestStation("Bearer $token", NearestStationRequest(
                        latitude, longitude, radiusM, alongRail = true,
                        candidates = candidates.takeIf { it.isNotEmpty() }
                            ?.map { NearestStationCandidate(it.key, it.name, it.latitude, it.longitude) },
                    ),
                )
            }.toRailResult()
        }
    } catch (e: TimeoutCancellationException) {
        RailLookupResult.Unavailable()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        RailLookupResult.Unavailable(e.response.headers["Retry-After"]?.trim()?.toLongOrNull()?.times(1000))
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
