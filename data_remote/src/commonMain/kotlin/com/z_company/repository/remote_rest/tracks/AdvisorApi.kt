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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /** Граф стыков и развилок участков (`GET /v1/advisor/graph`); без конверта - только топология и км. */
    suspend fun graph(): AdvisorGraphDto

    /** Путь плеча между станциями по графу стыков (`GET /v1/advisor/graph/path`); 404 `path_not_found` - станции не соединены. */
    suspend fun graphPath(fromStation: String, toStation: String): AdvisorGraphPathDto

    /** [token] - полный заголовок `Bearer ...`. */
    suspend fun postAltitudeContrib(token: String, body: AltitudeContribRequest): AltitudeContribResponse
}

class KtorAdvisorApi(
    private val client: HttpClient,
    /** Токен входа без префикса `Bearer`; null - пользователь не вошёл. Читается на каждый запрос. */
    private val bearerToken: suspend () -> String? = { null },
    /** Стабильный идентификатор аккаунта на устройстве (ключ Keystore и токен устройства привязаны к нему). */
    private val accountId: suspend () -> String? = { null },
    /** Ключ устройства в Keystore; null - платформа не поддерживает LD3. */
    private val deviceKeys: () -> DeviceKeys? = { null },
) : AdvisorApi {
    private suspend fun authorization(): String =
        bearerToken()?.takeIf { it.isNotBlank() }?.let { "Bearer $it" } ?: throw AdvisorApiException(401, "login_required")

    override suspend fun sections(): List<AdvisorSectionSummaryDto> {
        val auth = authorization()
        return call { client.get("v1/advisor/sections") { header("Authorization", auth) }.body() }
    }

    override suspend fun graph(): AdvisorGraphDto {
        val auth = authorization()
        return call { client.get("v1/advisor/graph") { header("Authorization", auth) }.body() }
    }

    override suspend fun graphPath(fromStation: String, toStation: String): AdvisorGraphPathDto {
        val auth = authorization()
        return call {
            client.get("v1/advisor/graph/path") {
                header("Authorization", auth)
                url { parameters.append("from_station", fromStation); parameters.append("to_station", toStation) }
            }.body()
        }
    }

    override suspend fun section(id: String): AdvisorSectionDto {
        val auth = authorization()
        val keys = deviceKeys()
        val account = accountId()?.takeIf { it.isNotBlank() } ?: "default"
        val blob = try {
            if (keys == null) throw AdvisorApiException(403, "device_unsupported")
            var b = fetchSection(id, auth, keys, account, deviceToken(auth, keys, account, forceRegister = false))
            if (b == null) {
                // Токен устройства просрочен/не принят: зарегистрировать ключ заново и повторить один раз.
                b = fetchSection(id, auth, keys, account, deviceToken(auth, keys, account, forceRegister = true))
                    ?: throw AdvisorApiException(403, "device_required")
            }
            b
        } catch (e: AdvisorApiException) {
            // Старый сервер со строгой аттестацией или устройство без пригодного Keystore-ключа:
            // слабый режим LD2 остаётся для обратной совместимости.
            if (e.status != 403) throw e
            fetchFallback(id, auth) ?: throw e
        }
        // Конверт не открылся (чужой формат, повреждён, ключ устройства потерян) - для вызывающего это сбой загрузки: останется кэш.
        val text = try {
            when {
                SectionSeal.isLd3(blob) -> SectionSeal.openLd3(blob, id) { keys?.unwrap(account, it) }
                else -> SectionSeal.open(blob, id)
            }
        } catch (e: SectionSeal.SealException) {
            keys?.clearToken(account)
            throw AdvisorApiException(502, "seal_failed")
        }
        return RemoteRestClient.appJson.decodeFromString(AdvisorSectionDto.serializer(), text)
    }

    /** Слабый режим: без токена устройства, когда регистрация Keystore-ключа не удалась. null - сервер его не отдаёт (403). */
    private suspend fun fetchFallback(id: String, auth: String): ByteArray? = try {
        call { client.get("v1/advisor/sections/$id") { header("Authorization", auth); header("X-Device-Fallback", "1") }.readRawBytes() }
    } catch (e: AdvisorApiException) {
        if (e.status == 403) null else throw e
    }

    /** Байты конверта либо null, если сервер требует (заново) зарегистрировать устройство. */
    private suspend fun fetchSection(id: String, auth: String, keys: DeviceKeys, account: String, token: String): ByteArray? = try {
        call { client.get("v1/advisor/sections/$id") { header("Authorization", auth); header("X-Device-Token", token) }.readRawBytes() }
    } catch (e: AdvisorApiException) {
        if (e.status == 403 && e.code == "device_required") { keys.clearToken(account); null } else throw e
    }

    /**
     * Регистрация ключа устройства идёт строго по одной и с памятью об отказе. Отказ регистрации (403)
     * постоянный: без паузы каждая загрузка участка заново делала challenge+регистрацию, а карта тянет до 8 участков и повторяет
     * каждые 30 с - лимиты регистрации уходили за минуты. Пауза - на аккаунт (смена аккаунта её не наследует); сбой сети паузу
     * не получает. Замок не даёт параллельным запросам регистрировать ключ одновременно, пока первый запрос не завершён.
     */
    private val registerLock = Mutex()
    private val registerBlockedUntil = HashMap<String, Long>()

    /** Токен устройства: из хранилища, пока не истёк, иначе сертификат ключа Keystore -> регистрация на сервере. */
    private suspend fun deviceToken(auth: String, keys: DeviceKeys, account: String, forceRegister: Boolean): String {
        fun stored(): String? {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            return keys.loadToken(account)?.takeIf { it.second > now + TOKEN_MARGIN_MS }?.first
        }
        if (!forceRegister) stored()?.let { return it }
        return registerLock.withLock {
            // Пока ждали замок, соседний запрос мог уже получить токен.
            if (!forceRegister) stored()?.let { return@withLock it }
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            if (now < (registerBlockedUntil[account] ?: 0L)) throw AdvisorApiException(403, "device_unsupported")
            try {
                register(auth, keys, account, forceRegister)
            } catch (e: AdvisorApiException) {
                // Постоянный отказ регистрации (403) и её лимит (429) - пауза; остальное (сеть, 5xx) повторяем как раньше.
                val pause = when (e.status) { 403 -> REGISTER_FAIL_PAUSE_MS; 429 -> REGISTER_LIMIT_PAUSE_MS; else -> 0L }
                if (pause > 0) registerBlockedUntil[account] = kotlin.time.Clock.System.now().toEpochMilliseconds() + pause
                throw e
            }
        }
    }

    private suspend fun register(auth: String, keys: DeviceKeys, account: String, forceRegister: Boolean): String {
        var forceNew = forceRegister
        repeat(2) { attempt ->
            val chain = keys.certificateChain(account, forceNew) ?: throw AdvisorApiException(403, "device_unsupported")
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
                // Совместимость со старым строгим сервером: один раз перевыпустить ключ.
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
        const val REGISTER_FAIL_PAUSE_MS = 30L * 60 * 1000
        const val REGISTER_LIMIT_PAUSE_MS = 10L * 60 * 1000
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: ResponseException) {
        val text = runCatching { e.response.bodyAsText() }.getOrNull()
        throw AdvisorApiException(e.response.status.value, KtorTracksApi.parseErrorCode(text) ?: advisorStringDetail(text))
    }
}

@Serializable
internal data class DeviceRegisterRequest(val chain: List<String>)

@Serializable
internal data class DeviceRegisterResponse(val deviceToken: String, val expiresAt: Double)

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
