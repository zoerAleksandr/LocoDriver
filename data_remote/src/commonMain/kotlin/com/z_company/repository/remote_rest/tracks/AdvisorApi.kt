package com.z_company.repository.remote_rest.tracks

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import com.z_company.repository.remote_rest.RemoteRestClient
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Ответ сервера с кодом ошибки (`detail.code`, например `axis_changed`). Без данных пользователя. */
class AdvisorApiException(val status: Int, val code: String?) : Exception("advisor api: $status ${code.orEmpty()}")

/**
 * Участки советника и обезличенный вклад высот. Участки отдаются только вошедшему пользователю и запечатанными (`SectionSeal`,
 * конверт LD2): без входа [sections] и [section] бросают [AdvisorApiException] 401 `login_required`, не обращаясь к серверу.
 */
interface AdvisorApi {
    suspend fun sections(): List<AdvisorSectionSummaryDto>
    suspend fun section(id: String): AdvisorSectionDto

    /** [token] - полный заголовок `Bearer ...`. */
    suspend fun postAltitudeContrib(token: String, body: AltitudeContribRequest): AltitudeContribResponse
}

class KtorAdvisorApi(
    private val client: HttpClient,
    /** Токен входа без префикса `Bearer`; null - пользователь не вошёл. Читается на каждый запрос. */
    private val bearerToken: suspend () -> String? = { null },
) : AdvisorApi {
    private suspend fun authorization(): String =
        bearerToken()?.takeIf { it.isNotBlank() }?.let { "Bearer $it" } ?: throw AdvisorApiException(401, "login_required")

    override suspend fun sections(): List<AdvisorSectionSummaryDto> {
        val auth = authorization()
        return call { client.get("v1/advisor/sections") { header("Authorization", auth) }.body() }
    }

    override suspend fun section(id: String): AdvisorSectionDto {
        val auth = authorization()
        val blob = call {
            val response: HttpResponse = client.get("v1/advisor/sections/$id") { header("Authorization", auth) }
            response.readRawBytes()
        }
        // Конверт не открылся (чужой формат, повреждён, неизвестный ключ) - для вызывающего это сбой загрузки: останется кэш.
        val text = try {
            SectionSeal.open(blob, id)
        } catch (e: SectionSeal.SealException) {
            throw AdvisorApiException(502, "seal_failed")
        }
        return RemoteRestClient.appJson.decodeFromString(AdvisorSectionDto.serializer(), text)
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
