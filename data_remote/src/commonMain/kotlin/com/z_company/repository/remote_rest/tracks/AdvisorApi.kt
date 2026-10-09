package com.z_company.repository.remote_rest.tracks

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Ответ сервера с кодом ошибки (`detail.code`, например `axis_changed`). Без данных пользователя. */
class AdvisorApiException(val status: Int, val code: String?) : Exception("advisor api: $status ${code.orEmpty()}")

/** Участки советника (только чтение, без входа) и обезличенный вклад высот (с JWT как защитой от спама). */
interface AdvisorApi {
    suspend fun sections(): List<AdvisorSectionSummaryDto>
    suspend fun section(id: String): AdvisorSectionDto

    /** [token] - полный заголовок `Bearer ...`. */
    suspend fun postAltitudeContrib(token: String, body: AltitudeContribRequest): AltitudeContribResponse
}

class KtorAdvisorApi(private val client: HttpClient) : AdvisorApi {
    override suspend fun sections(): List<AdvisorSectionSummaryDto> = call {
        client.get("v1/advisor/sections").body()
    }

    override suspend fun section(id: String): AdvisorSectionDto = call {
        client.get("v1/advisor/sections/$id").body()
    }

    override suspend fun postAltitudeContrib(token: String, body: AltitudeContribRequest): AltitudeContribResponse = call {
        client.post("v1/advisor/altitude-contrib") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: ResponseException) {
        val text = runCatching { e.response.bodyAsText() }.getOrNull()
        throw AdvisorApiException(e.response.status.value, KtorTracksApi.parseErrorCode(text))
    }
}
