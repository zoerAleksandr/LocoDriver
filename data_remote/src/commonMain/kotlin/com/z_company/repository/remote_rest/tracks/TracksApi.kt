package com.z_company.repository.remote_rest.tracks

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Ответ сервера с кодом ошибки. [code] — `detail.code` (например `consent_required`), без данных пользователя. */
class TracksApiException(val status: Int, val code: String?) : Exception("tracks api: $status ${code.orEmpty()}")

/**
 * Клиент `/v1/tracks`. Все методы бросают [TracksApiException] при HTTP-ошибке; транспортные сбои
 * (нет сети, таймаут) пробрасываются как есть. [token] — полный заголовок `Bearer ...`.
 */
interface TracksApi {
    suspend fun getConsent(token: String): TrackConsentResponse
    suspend fun putConsent(token: String, body: TrackConsentRequest): TrackConsentResponse
    suspend fun putTrack(token: String, trackId: String, body: TrackMetaRequest): TrackCardResponse
    suspend fun putChunk(token: String, trackId: String, chunkNo: Int, body: TrackChunkRequest): TrackChunkResponse
    suspend fun complete(token: String, trackId: String, body: TrackCompleteRequest): TrackCompleteResponse
    suspend fun list(token: String, limit: Int, cursor: String?, favorite: Boolean?): TrackListResponse
    suspend fun getTrack(token: String, trackId: String): TrackCardResponse
    suspend fun getChunk(token: String, trackId: String, chunkNo: Int): TrackChunkPayload
    suspend fun patch(token: String, trackId: String, body: TrackPatchRequest): TrackCardResponse

    /** Идемпотентно: 204 и для отсутствующего, и для чужого трека. */
    suspend fun delete(token: String, trackId: String)
    suspend fun deleteAll(token: String): TrackDeleteAllResponse
}

class KtorTracksApi(private val client: HttpClient) : TracksApi {

    override suspend fun getConsent(token: String): TrackConsentResponse = call {
        client.get("v1/tracks/consent") { header("Authorization", token) }.body()
    }

    override suspend fun putConsent(token: String, body: TrackConsentRequest): TrackConsentResponse = call {
        client.put("v1/tracks/consent") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()
    }

    override suspend fun putTrack(token: String, trackId: String, body: TrackMetaRequest): TrackCardResponse = call {
        client.put("v1/tracks/$trackId") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()
    }

    override suspend fun putChunk(
        token: String,
        trackId: String,
        chunkNo: Int,
        body: TrackChunkRequest,
    ): TrackChunkResponse = call {
        client.put("v1/tracks/$trackId/chunks/$chunkNo") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()
    }

    override suspend fun complete(token: String, trackId: String, body: TrackCompleteRequest): TrackCompleteResponse = call {
        client.post("v1/tracks/$trackId/complete") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()
    }

    override suspend fun list(token: String, limit: Int, cursor: String?, favorite: Boolean?): TrackListResponse = call {
        client.get("v1/tracks") {
            header("Authorization", token)
            parameter("limit", limit)
            if (!cursor.isNullOrBlank()) parameter("cursor", cursor)
            if (favorite != null) parameter("favorite", favorite)
        }.body()
    }

    override suspend fun getTrack(token: String, trackId: String): TrackCardResponse = call {
        client.get("v1/tracks/$trackId") { header("Authorization", token) }.body()
    }

    override suspend fun getChunk(token: String, trackId: String, chunkNo: Int): TrackChunkPayload = call {
        client.get("v1/tracks/$trackId/chunks/$chunkNo") { header("Authorization", token) }.body()
    }

    override suspend fun patch(token: String, trackId: String, body: TrackPatchRequest): TrackCardResponse = call {
        client.patch("v1/tracks/$trackId") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()
    }

    override suspend fun delete(token: String, trackId: String) {
        call<Unit> {
            client.delete("v1/tracks/$trackId") { header("Authorization", token) }
            Unit
        }
    }

    override suspend fun deleteAll(token: String): TrackDeleteAllResponse = call {
        client.delete("v1/tracks") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(TrackDeleteAllRequest(confirm = true))
        }.body()
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: ResponseException) {
        val text = runCatching { e.response.bodyAsText() }.getOrNull()
        throw TracksApiException(e.response.status.value, parseErrorCode(text))
    }

    companion object {
        /** `{"detail":{"code":"consent_required"}}` -> `consent_required`; иначе null. */
        internal fun parseErrorCode(body: String?): String? {
            if (body.isNullOrBlank()) return null
            return runCatching {
                val detail = Json.parseToJsonElement(body).jsonObject["detail"]
                (detail as? JsonObject)?.get("code")?.jsonPrimitive?.content
            }.getOrNull()
        }
    }
}
