package com.z_company.repository.remote_rest.tracks

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import com.z_company.repository.remote_rest.RemoteRestClient
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
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
    /** Стабильный идентификатор аккаунта на устройстве (ключ Keystore и токен устройства привязаны к нему). */
    private val accountId: suspend () -> String? = { null },
    /** Ключ устройства (аттестация); null - платформа не поддерживает (участки не открываются). */
    private val deviceKeys: () -> DeviceKeys? = { null },
) : AdvisorApi {
    private suspend fun authorization(): String =
        bearerToken()?.takeIf { it.isNotBlank() }?.let { "Bearer $it" } ?: throw AdvisorApiException(401, "login_required")

    override suspend fun sections(): List<AdvisorSectionSummaryDto> {
        val auth = authorization()
        return call { client.get("v1/advisor/sections") { header("Authorization", auth) }.body() }
    }

    override suspend fun section(id: String): AdvisorSectionDto {
        val auth = authorization()
        val keys = deviceKeys() ?: throw AdvisorApiException(403, "device_unsupported")
        val account = accountId()?.takeIf { it.isNotBlank() } ?: "default"
        var blob = fetchSection(id, auth, keys, account, deviceToken(auth, keys, account, forceRegister = false))
        if (blob == null) {
            // Токен устройства просрочен/не принят: пройти аттестацию заново и повторить один раз.
            blob = fetchSection(id, auth, keys, account, deviceToken(auth, keys, account, forceRegister = true))
                ?: throw AdvisorApiException(403, "device_required")
        }
        // Конверт не открылся (чужой формат, повреждён, ключ устройства потерян) - для вызывающего это сбой загрузки: останется кэш.
        val text = try {
            when {
                SectionSeal.isLd3(blob) -> SectionSeal.openLd3(blob, id) { keys.unwrap(account, it) }
                else -> SectionSeal.open(blob, id)
            }
        } catch (e: SectionSeal.SealException) {
            keys.clearToken(account)
            throw AdvisorApiException(502, "seal_failed")
        }
        return RemoteRestClient.appJson.decodeFromString(AdvisorSectionDto.serializer(), text)
    }

    /** Байты конверта либо null, если сервер требует (заново) зарегистрировать устройство. */
    private suspend fun fetchSection(id: String, auth: String, keys: DeviceKeys, account: String, token: String): ByteArray? = try {
        call { client.get("v1/advisor/sections/$id") { header("Authorization", auth); header("X-Device-Token", token) }.readRawBytes() }
    } catch (e: AdvisorApiException) {
        if (e.status == 403 && e.code == "device_required") { keys.clearToken(account); null } else throw e
    }

    /** Токен устройства: из хранилища, пока не истёк, иначе challenge -> аттестация ключа в Keystore -> регистрация на сервере. */
    private suspend fun deviceToken(auth: String, keys: DeviceKeys, account: String, forceRegister: Boolean): String {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        if (!forceRegister) keys.loadToken(account)?.takeIf { it.second > now + TOKEN_MARGIN_MS }?.let { return it.first }
        var forceNew = forceRegister
        repeat(2) { attempt ->
            val challenge = call { client.get("v1/advisor/device/challenge") { header("Authorization", auth) }.body<DeviceChallengeDto>() }
            val chain = keys.attest(account, decodeBase64(challenge.challenge), forceNew) ?: throw AdvisorApiException(403, "device_unsupported")
            try {
                val reg = call {
                    client.post("v1/advisor/device") {
                        contentType(ContentType.Application.Json)
                        header("Authorization", auth)
                        setBody(DeviceRegisterRequest(chain.map { encodeBase64(it) }))
                    }.body<DeviceRegisterResponse>()
                }
                keys.saveToken(account, reg.deviceToken, reg.expiresAt.toLong())
                return reg.deviceToken
            } catch (e: AdvisorApiException) {
                // Ключ выпущен под другой challenge (сменили секрет сервера / аккаунт) - один раз перевыпустить.
                if (attempt == 0 && e.status == 403 && e.code == "challenge_mismatch") forceNew = true else throw e
            }
        }
        throw AdvisorApiException(403, "device_unsupported")
    }

    override suspend fun postAltitudeContrib(token: String, body: AltitudeContribRequest): AltitudeContribResponse = call {
        client.post("v1/advisor/altitude-contrib") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()
    }

    private companion object {
        const val TOKEN_MARGIN_MS = 24L * 3600 * 1000
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: ResponseException) {
        val text = runCatching { e.response.bodyAsText() }.getOrNull()
        throw AdvisorApiException(e.response.status.value, KtorTracksApi.parseErrorCode(text) ?: advisorStringDetail(text))
    }
}

@Serializable
internal data class DeviceChallengeDto(val challenge: String)

@Serializable
internal data class DeviceRegisterRequest(val chain: List<String>)

@Serializable
internal data class DeviceRegisterResponse(val deviceToken: String, val expiresAt: Double)

@OptIn(ExperimentalEncodingApi::class)
internal fun decodeBase64(s: String): ByteArray = Base64.decode(s)

@OptIn(ExperimentalEncodingApi::class)
internal fun encodeBase64(b: ByteArray): String = Base64.encode(b)

/** `{"detail":"device_required"}` -> `device_required` (эндпоинты советника отдают код строкой); иначе null. */
internal fun advisorStringDetail(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return runCatching {
        (kotlinx.serialization.json.Json.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject)
            ?.get("detail")?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.takeIf { it.isString }?.content
    }.getOrNull()
}
