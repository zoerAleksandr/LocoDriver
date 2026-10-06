package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.response.LatLon
import com.z_company.repository.remote_rest.response.OsmPathStation
import com.z_company.repository.remote_rest.response.OsmStationCandidate
import com.z_company.repository.remote_rest.response.RouteLengthRequest
import com.z_company.repository.remote_rest.response.RouteLengthResponse
import com.z_company.repository.remote_rest.response.ServicePhaseLookupRequest
import com.z_company.repository.remote_rest.response.ServicePhaseSuggestRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException

/** Какая крайняя станция плеча. */
enum class PhaseEnd { DEPARTURE, ARRIVAL }

/**
 * NO_INTERNET — у телефона нет сети; NO_CONNECTION — сеть есть, но запрос не дошёл (обрыв, сервер
 * недоступен — что именно, по телефону не понять, поэтому текст без утверждений);
 * VPN — то же при включённом VPN; SERVICE — сервер ответил ошибкой или не уложился в 30 с.
 */
enum class AutoFillFailure { NO_INTERNET, NO_CONNECTION, VPN, STATION_NOT_FOUND, NO_PATH, SERVICE, RATE_LIMITED }

/** Короткий текст сетевой/серверной ошибки для снекбаров и подписей. */
fun AutoFillFailure.message(): String = when (this) {
    AutoFillFailure.NO_INTERNET -> "Нет подключения к интернету"
    AutoFillFailure.NO_CONNECTION -> "Не удалось связаться с сервером — проверьте подключение к интернету"
    AutoFillFailure.VPN -> com.z_company.core.util.VPN_ERROR_HINT
    AutoFillFailure.RATE_LIMITED -> "Слишком много запросов — попробуйте позже"
    else -> "Сервис не ответил — попробуйте позже"
}

/** Вариант пути между крайними станциями плеча (route/length, alternatives). [via] — станции по пути. */
data class RouteVariant(val id: String, val lengthKm: Double, val line: List<Pair<Double, Double>>, val via: List<RouteVia>) {
    /** «Через …» в чипе: первая станция по пути (сервер отдаёт только станции, не о.п.); нет — без подписи. */
    val firstVia: String? get() = via.firstOrNull()?.name
}

data class RouteVia(
    val name: String,
    val km: Double,
    val osmId: Long?,
    val esr: String?,
    val latitude: Double?,
    val longitude: Double?,
)

sealed interface AutoFillState {
    data object Idle : AutoFillState

    /** step: 0 — ищем крайние станции, 1 — прокладываем путь, 2 — собираем станции. */
    data class Loading(val step: Int) : AutoFillState

    /** [approximate] — точного совпадения нет, варианты похожи по сокращению. */
    data class Choose(
        val end: PhaseEnd,
        val query: String,
        val candidates: List<OsmStationCandidate>,
        val approximate: Boolean = false,
    ) : AutoFillState

    data class Ready(
        val departure: OsmStationCandidate,
        val arrival: OsmStationCandidate,
        val lengthKm: Double,
        val stations: List<OsmPathStation>,
    ) : AutoFillState

    data class Failed(val failure: AutoFillFailure, val end: PhaseEnd? = null, val name: String? = null) : AutoFillState
}

internal fun preferReverseAlternatives(
    forward: RouteLengthResponse,
    reverse: RouteLengthResponse,
): RouteLengthResponse {
    if (reverse.routes.size <= forward.routes.size) return forward
    return reverseRouteResponse(reverse)
}

internal fun reverseRouteResponse(reverse: RouteLengthResponse): RouteLengthResponse {
    val routes = reverse.routes.map { route ->
        route.copy(
            geometry = route.geometry.asReversed(),
            viaStations = route.viaStations.asReversed().map { via ->
                via.copy(km = (route.lengthKm - via.km).coerceIn(0.0, route.lengthKm))
            },
        )
    }
    return reverse.copy(
        kmAt = reverse.kmAt.asReversed().map { km ->
            (reverse.lengthKm - km).coerceIn(0.0, reverse.lengthKm)
        },
        geometry = reverse.geometry.asReversed(),
        routes = routes,
    )
}

/**
 * Автоподбор промежуточных станций плеча: сервер (`/v1/service_phase/stations/suggest`)
 * ищет крайние станции и путь по OSM. Выбор неоднозначной станции запоминается
 * osmId и уходит в повторный запрос. Таймаут — 30 с (SCREEN_SPECS §6.6).
 */
class ServicePhaseAutoFillViewModel : ViewModel(), KoinComponent {
    private val api: RemoteRestApi by inject()
    private val tokens: SecureTokenStorage by inject()
    private val appContext: android.content.Context by inject()

    private val _state = MutableStateFlow<AutoFillState>(AutoFillState.Idle)
    val state = _state.asStateFlow()

    private var job: Job? = null
    private var request: ServicePhaseSuggestRequest? = null

    fun start(
        departure: String,
        arrival: String,
        country: String,
        departureOsmId: Long?,
        arrivalOsmId: Long?,
        departurePoint: Pair<Double, Double>? = null,
        arrivalPoint: Pair<Double, Double>? = null,
    ) {
        request = ServicePhaseSuggestRequest(
            departureStation = departure.trim(),
            arrivalStation = arrival.trim(),
            country = country.ifBlank { "RU" },
            departureOsmId = departureOsmId,
            arrivalOsmId = arrivalOsmId,
            departurePoint = departurePoint?.let { (lat, lon) -> LatLon(lat, lon) },
            arrivalPoint = arrivalPoint?.let { (lat, lon) -> LatLon(lat, lon) },
        )
        run()
    }

    fun choose(end: PhaseEnd, candidate: OsmStationCandidate) {
        val r = request ?: return
        request = when (end) {
            PhaseEnd.DEPARTURE -> r.copy(departureOsmId = candidate.osmId)
            PhaseEnd.ARRIVAL -> r.copy(arrivalOsmId = candidate.osmId)
        }
        run()
    }

    fun retry() = run()

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = AutoFillState.Idle
    }

    private fun run() {
        val r = request ?: return
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = AutoFillState.Loading(0)
            // Шаги — для ощущения хода работы: сервер отвечает одним ответом.
            val ticker = launch {
                delay(1_200)
                _state.value = AutoFillState.Loading(1)
                delay(2_500)
                _state.value = AutoFillState.Loading(2)
            }
            val result = try {
                val token = tokens.getAuthBearerTokenFlow().first()
                withTimeout(TIMEOUT_MS) { api.suggestServicePhaseStations("Bearer $token", r) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                fail(ticker, failureOf(e))
                return@launch
            } catch (e: Exception) {
                fail(ticker, failureOf(e))
                return@launch
            }
            ticker.cancel()
            val end = if (result.side == "arrival") PhaseEnd.ARRIVAL else PhaseEnd.DEPARTURE
            _state.value = when (result.status) {
                "ok" -> {
                    val dep = result.departure
                    val arr = result.arrival
                    if (dep != null && arr != null) {
                        AutoFillState.Ready(dep, arr, result.lengthKm ?: 0.0, result.stations)
                    } else AutoFillState.Failed(AutoFillFailure.SERVICE)
                }
                "ambiguous" -> AutoFillState.Choose(
                    end, result.name ?: "", result.candidates,
                    approximate = result.approximate || result.candidates.any { it.approximate },
                )
                "not_found" -> AutoFillState.Failed(AutoFillFailure.STATION_NOT_FOUND, end, result.name)
                "no_path" -> AutoFillState.Failed(AutoFillFailure.NO_PATH)
                else -> AutoFillState.Failed(AutoFillFailure.SERVICE)
            }
        }
    }

    private fun fail(ticker: Job, failure: AutoFillFailure) {
        ticker.cancel()
        _state.value = AutoFillState.Failed(failure)
    }

    sealed interface MeasureResult {
        /** [variants] — варианты пути (первый — кратчайший); пусто — вариант один. */
        data class Ok(val lengthKm: Double, val kmAt: List<Double>, val variants: List<RouteVariant> = emptyList()) : MeasureResult
        data object NoPath : MeasureResult
        data class Failed(val failure: AutoFillFailure) : MeasureResult
    }

    /**
     * «Измерить по карте»: длина по ж/д путям через точки по порядку (отправление … прибытие).
     * [withVariants] — запросить до 3 вариантов пути (только когда промежуточных станций нет).
     * [withGeometry] — вернуть линию пути для предпросмотра, даже если вариант один.
     */
    suspend fun measure(
        points: List<Pair<Double, Double>>,
        withVariants: Boolean = false,
        withGeometry: Boolean = withVariants,
    ): MeasureResult = try {
        val token = tokens.getAuthBearerTokenFlow().first()
        val res = withTimeout(TIMEOUT_MS) {
            val request = RouteLengthRequest(
                points = points.map { (lat, lon) -> LatLon(lat, lon) },
                withGeometry = withGeometry,
                alternatives = if (withVariants) MAX_VARIANTS else 1,
            )
            // Альтернативы в серверном графе могут зависеть от направления. Для пары крайних
            // станций всегда используем один канонический порядок, чтобы A → B и B → A давали
            // один набор путей без второго тяжёлого запроса.
            if (withVariants && request.points.size == 2) {
                val first = request.points[0]
                val second = request.points[1]
                val requestedIsCanonical = first.latitude < second.latitude ||
                    (first.latitude == second.latitude && first.longitude <= second.longitude)
                val canonicalRequest = if (requestedIsCanonical) request
                else request.copy(points = request.points.asReversed())
                val canonical = api.measureRouteLength("Bearer $token", canonicalRequest)
                if (requestedIsCanonical) canonical else reverseRouteResponse(canonical)
            } else {
                api.measureRouteLength(
                    "Bearer $token",
                    request,
                )
            }
        }
        // Геометрия единственного маршрута тоже нужна клиенту: после выбора пользовательской
        // via-точки показываем путь на карте до того, как пользователь его подтвердит.
        val variants = res.routes.mapNotNull(::toVariant).ifEmpty {
            if (withGeometry && res.geometry.size >= 2) {
                listOf(
                    RouteVariant(
                        id = "single",
                        lengthKm = res.lengthKm,
                        line = res.geometry.mapNotNull { point ->
                            if (point.size >= 2) point[0] to point[1] else null
                        },
                        via = emptyList(),
                    )
                ).filter { it.line.size >= 2 }
            } else {
                emptyList()
            }
        }
        MeasureResult.Ok(res.lengthKm, res.kmAt, variants)
    } catch (e: kotlinx.coroutines.CancellationException) {
        if (e !is TimeoutCancellationException) throw e
        MeasureResult.Failed(failureOf(e))
    } catch (e: Exception) {
        val msg = e.message.orEmpty()
        // 404 «Not Found» — эндпоинта ещё нет на сервере; наш 404 — «путь не найден».
        if (msg.contains("404") && !msg.contains("Not Found")) MeasureResult.NoPath
        else MeasureResult.Failed(failureOf(e))
    }

    /**
     * Причина сбоя запроса: 429 — лимит; сетевая ошибка — нет интернета / нет связи с сервером /
     * VPN (по состоянию сети телефона); иначе — ошибка сервиса. Таймаут 30 с при живом
     * интернете — «сервис не ответил».
     */
    private fun failureOf(e: Throwable): AutoFillFailure {
        val chain = generateSequence(e) { it.cause }.take(6).toList()
        android.util.Log.w("ServicePhaseNet", "request failed: " + chain.joinToString(" <- ") { it::class.simpleName.orEmpty() })
        val text = chain.joinToString(" ") { "${it::class.simpleName} ${it.message.orEmpty()}" }
        if (text.contains("429")) return AutoFillFailure.RATE_LIMITED
        val network = e is TimeoutCancellationException || chain.any { it is IOException } ||
            com.z_company.core.util.isConnectivityErrorMessage(text) ||
            NETWORK_MARKERS.any { text.contains(it) }
        if (!network) return AutoFillFailure.SERVICE
        return when {
            !hasInternet() -> AutoFillFailure.NO_INTERNET
            e is TimeoutCancellationException -> AutoFillFailure.SERVICE
            com.z_company.core.util.isVpnActive(appContext) -> AutoFillFailure.VPN
            else -> AutoFillFailure.NO_CONNECTION
        }
    }

    private fun hasInternet(): Boolean = runCatching {
        val cm = appContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(true)

    /** Причина последней неудачи [lookup] (null — последний поиск удался). */
    var lastLookupFailure: AutoFillFailure? = null
        private set

    /** Линия плеча по рельсам: [Ok] — точки линии; [NoPath] — путь не найден; [Failed] — сеть/сервер. */
    sealed interface RailLineResult {
        data class Ok(val line: List<Pair<Double, Double>>, val fromCache: Boolean = false) : RailLineResult
        data object NoPath : RailLineResult
        data class Failed(val failure: AutoFillFailure) : RailLineResult
    }

    /**
     * Линия плеча по рельсам для карты (через точки по порядку): всегда запрашивается с сервера
     * (один повтор через 2 с при сбое); последняя полученная хранится на устройстве по набору
     * точек и рисуется, только если сервер недоступен.
     */
    suspend fun railLine(points: List<Pair<Double, Double>>): RailLineResult {
        val key = points.joinToString(";") { (lat, lon) -> "%.5f,%.5f".format(java.util.Locale.US, lat, lon) }
        val file = java.io.File(appContext.noBackupFilesDir, "rail_lines/${key.hashCode().toUInt().toString(16)}.txt")
        // Всегда сначала сервер: граф путей обновляется, и сохранённая линия могла устареть
        // (так было после добавления станционных путей — карта рисовала старый объезд).
        // Кэш — только запас, когда сервер недоступен.
        var result: RailLineResult = RailLineResult.Failed(AutoFillFailure.SERVICE)
        for (attempt in 0..1) {
            if (attempt > 0) delay(2_000)
            result = try {
                val token = tokens.getAuthBearerTokenFlow().first()
                val line = withTimeout(TIMEOUT_MS) {
                    api.measureRouteLength(
                        "Bearer $token",
                        com.z_company.repository.remote_rest.response.RouteLengthRequest(
                            points = points.map { (lat, lon) -> LatLon(lat, lon) },
                            withGeometry = true,
                        ),
                    ).geometry.mapNotNull { p -> if (p.size >= 2) p[0] to p[1] else null }
                }
                if (line.size >= 2) RailLineResult.Ok(line) else RailLineResult.NoPath
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                RailLineResult.Failed(failureOf(e))
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                android.util.Log.w("ServicePhaseMap", "rail line: ${e::class.simpleName}")
                if (msg.contains("404") && !msg.contains("Not Found")) RailLineResult.NoPath
                else RailLineResult.Failed(failureOf(e))
            }
            // Повторяем только сбой сети/сервера: «путь не найден» не изменится.
            if (result !is RailLineResult.Failed || (result as RailLineResult.Failed).failure == AutoFillFailure.RATE_LIMITED) break
        }
        val ok = result as? RailLineResult.Ok
            ?: return (if (result is RailLineResult.Failed) withContext(Dispatchers.IO) { readRailLine(file, key) } else null)
                ?.let { RailLineResult.Ok(it, fromCache = true) }
                ?: result
        withContext(Dispatchers.IO) {
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(key + "\n" + ok.line.joinToString(";") { (lat, lon) -> "$lat,$lon" })
            }
        }
        return ok
    }

    private fun readRailLine(file: java.io.File, key: String): List<Pair<Double, Double>>? = runCatching {
        if (!file.exists()) return null
        val (head, body) = file.readText().split("\n", limit = 2).takeIf { it.size == 2 } ?: return null
        if (head != key) return null
        body.split(";").mapNotNull { p ->
            val (lat, lon) = p.split(",").takeIf { it.size == 2 } ?: return@mapNotNull null
            lat.toDoubleOrNull()?.let { a -> lon.toDoubleOrNull()?.let { a to it } }
        }.takeIf { it.size >= 2 }
    }.getOrNull()

    /** Координаты одной станции (шторка станции, «Найти координаты»). null — ошибка сети/сервиса. */
    suspend fun lookup(name: String, country: String, near: List<Pair<Double, Double>>): List<OsmStationCandidate>? =
        try {
            lastLookupFailure = null
            val token = tokens.getAuthBearerTokenFlow().first()
            withTimeout(TIMEOUT_MS) {
                api.lookupServicePhaseStation(
                    "Bearer $token",
                    ServicePhaseLookupRequest(
                        name = name.trim(),
                        country = country.ifBlank { "RU" },
                        near = near.map { (lat, lon) -> LatLon(lat, lon) },
                    ),
                ).candidates
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            lastLookupFailure = failureOf(e)
            null
        } catch (e: Exception) {
            lastLookupFailure = failureOf(e)
            null
        }

    private fun toVariant(r: com.z_company.repository.remote_rest.response.RouteVariantDto): RouteVariant? {
        val line = r.geometry.mapNotNull { p -> if (p.size >= 2) p[0] to p[1] else null }
        if (line.size < 2) return null
        return RouteVariant(
            id = r.id,
            lengthKm = r.lengthKm,
            line = line,
            via = r.viaStations.map { RouteVia(it.name, it.km, it.osmId, it.esr, it.latitude, it.longitude) },
        )
    }

    private companion object {
        const val MAX_VARIANTS = 3
        const val TIMEOUT_MS = 30_000L
        /** Сетевые исключения Ktor/JVM, которые не наследуют IOException. */
        val NETWORK_MARKERS = listOf("UnresolvedAddress", "UnknownHost", "ConnectException", "NoRouteToHost",
            "SocketTimeout", "ConnectTimeout", "HttpRequestTimeout", "SSLException", "ClosedReceiveChannel")
    }
}
