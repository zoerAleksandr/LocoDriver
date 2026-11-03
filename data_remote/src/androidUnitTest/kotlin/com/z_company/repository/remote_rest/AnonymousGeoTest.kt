package com.z_company.repository.remote_rest

import com.z_company.domain.util.RailLookupResult
import com.z_company.repository.remote_rest.response.LatLon
import com.z_company.repository.remote_rest.response.NearestStationRequest
import com.z_company.repository.remote_rest.response.RouteLengthRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.callContext
import io.ktor.client.engine.HttpClientEngineCapability
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Гео-эндпоинты без аккаунта (SCREEN_SPECS §6.9): нет токена - нет Authorization; 401 - повтор без токена; 429 - пауза. */
@OptIn(io.ktor.utils.io.InternalAPI::class)
class AnonymousGeoTest {

    private data class Answer(val status: HttpStatusCode, val body: String, val headers: Map<String, String> = emptyMap())

    /** Движок-заглушка: отвечает по очереди, запоминает заголовок Authorization каждого запроса. */
    private class FakeEngine(private val answers: MutableList<Answer>) : HttpClientEngineBase("fake") {
        val auth = mutableListOf<String?>()
        override val config = HttpClientEngineConfig()
        override val supportedCapabilities: Set<HttpClientEngineCapability<*>> = emptySet()

        override suspend fun execute(data: HttpRequestData): HttpResponseData {
            auth += data.headers[HttpHeaders.Authorization]
            val a = answers.removeAt(0)
            val hs = headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                *a.headers.map { it.key to listOf(it.value) }.toTypedArray(),
            )
            return HttpResponseData(a.status, GMTDate(), hs, HttpProtocolVersion.HTTP_1_1, ByteReadChannel(a.body), callContext())
        }
    }

    private fun fake(vararg a: Answer) = FakeEngine(a.toMutableList())

    private fun apiOver(engine: FakeEngine) = KtorRemoteRestApi(
        HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(RemoteRestClient.appJson) }
            installSessionExpiredDetection()
        },
    )

    private val ok = Answer(HttpStatusCode.OK, """{"station":null,"reason":"off_rails","alongRail":true}""")
    private val unauthorized = Answer(HttpStatusCode.Unauthorized, """{"detail":"Not authenticated"}""")
    private val limited = Answer(HttpStatusCode.TooManyRequests, """{"detail":"Слишком много запросов"}""", mapOf("Retry-After" to "120"))
    private val req = NearestStationRequest(59.0, 30.0, alongRail = true)

    @AfterTest fun reset() = GeoRateLimit.reset()

    @Test fun `no token - request goes without Authorization`() = runBlocking {
        val e = fake(ok)
        apiOver(e).nearestStation(null, req)
        assertEquals(listOf<String?>(null), e.auth)
    }

    @Test fun `token present - Authorization as before`() = runBlocking {
        val e = fake(ok)
        apiOver(e).nearestStation("Bearer abc", req)
        assertEquals(listOf<String?>("Bearer abc"), e.auth)
    }

    @Test fun `401 with token - one retry without token`() = runBlocking {
        val e = fake(unauthorized, ok)
        apiOver(e).nearestStation("Bearer stale", req)
        assertEquals(listOf<String?>("Bearer stale", null), e.auth)
    }

    /** Считает сигналы SessionExpiredNotifier за время блока. */
    private suspend fun countSignals(block: suspend () -> Unit): Int = kotlinx.coroutines.coroutineScope {
        var n = 0
        val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) { SessionExpiredNotifier.events.collect { n++ } }
        try { block() } catch (_: Exception) {} finally { job.cancel() }
        n
    }

    @Test fun `401 with token on geo request - no session-expired signal when anonymous retry succeeds`() = runBlocking {
        val e = fake(unauthorized, ok)
        val signals = countSignals { apiOver(e).nearestStation("Bearer stale", req) }
        assertEquals(0, signals)
        assertEquals(listOf<String?>("Bearer stale", null), e.auth)
    }

    @Test fun `401 with token on non-geo request - session-expired signal as before`() = runBlocking {
        val e = fake(unauthorized)
        val signals = countSignals { apiOver(e).getReferralStatus("Bearer stale") }
        assertEquals(1, signals)
    }

    @Test fun `401 without token - no retry, error propagates`() {
        val e = fake(unauthorized, ok)
        assertFailsWith<io.ktor.client.plugins.ClientRequestException> {
            runBlocking {
                apiOver(e).measureRouteLength(null, RouteLengthRequest(points = listOf(LatLon(1.0, 2.0), LatLon(3.0, 4.0))))
            }
        }
        assertEquals(1, e.auth.size)
    }

    @Test fun `429 is not retried`() {
        val e = fake(limited, ok)
        assertFailsWith<io.ktor.client.plugins.ClientRequestException> {
            runBlocking { apiOver(e).nearestStation(null, req) }
        }
        assertEquals(1, e.auth.size)
    }

    @Test fun `429 in rail lookup - Unavailable with Retry-After and pause without new requests`() = runBlocking {
        val e = fake(limited, ok)
        val lookup = RemoteRailStationLookup(apiOver(e), bearerToken = { null })
        val first = assertIs<RailLookupResult.Unavailable>(lookup.nearestAlongRail(59.0, 30.0, 500.0, emptyList()))
        assertEquals(120_000L, first.retryAfterMs)
        assertTrue(GeoRateLimit.remainingMs() in 1..120_000L)
        // Пауза: второй вызов не доходит до сервера.
        assertIs<RailLookupResult.Unavailable>(lookup.nearestAlongRail(59.0, 30.0, 500.0, emptyList()))
        assertEquals(1, e.auth.size)
    }

    @Test fun `rail lookup without token reaches server anonymously`() = runBlocking {
        val e = fake(ok)
        val r = RemoteRailStationLookup(apiOver(e), bearerToken = { null }).nearestAlongRail(59.0, 30.0, 500.0, emptyList())
        assertIs<RailLookupResult.NoStation>(r)
        assertNull(e.auth.single())
    }
}
