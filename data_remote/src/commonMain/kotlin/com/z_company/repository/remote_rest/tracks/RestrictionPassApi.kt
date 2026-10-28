package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.RestrictionPass
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/** Тело `POST /v1/restriction_passes/`: до [MAX_BATCH] записей (контракт - SCREEN_SPECS, «Журнал проследования»). */
@Serializable
data class RestrictionPassBatchRequest(val passes: List<RestrictionPass>)

@Serializable
data class RestrictionPassSkipped(val trainId: String = "", val warningId: String = "", val reason: String = "")

@Serializable
data class RestrictionPassBatchResponse(val accepted: Int = 0, val skipped: List<RestrictionPassSkipped> = emptyList())

@Serializable
data class RestrictionPassListResponse(val passes: List<RestrictionPass> = emptyList(), val nextCursor: String? = null)

/** HTTP-ошибка журнала; [code] - `detail.code` (`consent_required`, `rate_limited`...), без данных пользователя. */
class RestrictionPassApiException(val status: Int, val code: String?) : Exception("restriction passes api: $status ${code.orEmpty()}")

interface RestrictionPassApi {
    suspend fun put(token: String, passes: List<RestrictionPass>): RestrictionPassBatchResponse

    /** Записи (с надгробиями) строго после [cursor] / не старше [sinceMs]; [limit] <= [MAX_BATCH]. */
    suspend fun list(token: String, cursor: String?, sinceMs: Long?, limit: Int): RestrictionPassListResponse

    companion object {
        const val MAX_BATCH = 100
    }
}

class KtorRestrictionPassApi(private val client: HttpClient) : RestrictionPassApi {
    override suspend fun put(token: String, passes: List<RestrictionPass>): RestrictionPassBatchResponse = call {
        client.post("v1/restriction_passes/") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(RestrictionPassBatchRequest(passes))
        }.body()
    }

    override suspend fun list(token: String, cursor: String?, sinceMs: Long?, limit: Int): RestrictionPassListResponse = call {
        client.get("v1/restriction_passes/") {
            header("Authorization", token)
            parameter("limit", limit)
            if (!cursor.isNullOrBlank()) parameter("cursor", cursor)
            else if (sinceMs != null) parameter("since", sinceMs)
        }.body()
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: ResponseException) {
        val text = runCatching { e.response.bodyAsText() }.getOrNull()
        throw RestrictionPassApiException(e.response.status.value, KtorTracksApi.parseErrorCode(text))
    }
}
