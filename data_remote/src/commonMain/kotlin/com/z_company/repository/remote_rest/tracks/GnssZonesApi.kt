package com.z_company.repository.remote_rest.tracks

import com.z_company.repository.remote_rest.GeoRequestKey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/** Ошибка ответа (`detail.code`, например `too_many_cells`). Без данных пользователя. */
class GnssZonesApiException(val status: Int, val code: String?) : Exception("gnss zones api: $status ${code.orEmpty()}")

/** Область запроса, градусы. Сервер: каждая сторона не больше 1°. */
data class GnssBbox(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double)

/** Результат `GET`: 304 (кэш актуален) или данные с новым ETag. */
sealed interface GnssZonesFetch {
    data object NotModified : GnssZonesFetch
    data class Data(val etag: String?, val body: GnssZonesResponse) : GnssZonesFetch
}

/** Общая карта зон без GPS и обезличенный вклад (оба метода - с JWT; [token] - полный заголовок `Bearer ...`). */
interface GnssZonesApi {
    suspend fun postContrib(token: String, body: GnssZoneContribRequest): GnssZoneContribResponse
    suspend fun zones(token: String, bbox: GnssBbox, etag: String?): GnssZonesFetch
}

class KtorGnssZonesApi(private val client: HttpClient) : GnssZonesApi {
    override suspend fun postContrib(token: String, body: GnssZoneContribRequest): GnssZoneContribResponse = call {
        client.post("v1/gnss/zones/contrib") {
            // 401 здесь не означает конец сессии (необязательная функция): разлогина быть не должно.
            attributes.put(GeoRequestKey, true)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, token)
            setBody(body)
        }.body()
    }

    override suspend fun zones(token: String, bbox: GnssBbox, etag: String?): GnssZonesFetch = call {
        val response = client.get("v1/gnss/zones") {
            attributes.put(GeoRequestKey, true)
            // 304 разбираем сами: по умолчанию 3xx - исключение.
            expectSuccess = false
            header(HttpHeaders.Authorization, token)
            if (etag != null) header(HttpHeaders.IfNoneMatch, etag)
            parameter("minLat", bbox.minLat)
            parameter("minLon", bbox.minLon)
            parameter("maxLat", bbox.maxLat)
            parameter("maxLon", bbox.maxLon)
        }
        when {
            response.status == HttpStatusCode.NotModified -> GnssZonesFetch.NotModified
            response.status.value in 200..299 ->
                GnssZonesFetch.Data(response.headers[HttpHeaders.ETag], response.body())
            else -> throw GnssZonesApiException(
                response.status.value,
                KtorTracksApi.parseErrorCode(runCatching { response.bodyAsText() }.getOrNull()),
            )
        }
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: ResponseException) {
        val text = runCatching { e.response.bodyAsText() }.getOrNull()
        throw GnssZonesApiException(e.response.status.value, KtorTracksApi.parseErrorCode(text))
    }
}
