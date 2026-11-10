package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.GeoRateLimit
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.response.LatLon
import com.z_company.repository.remote_rest.response.OsmPathStation
import com.z_company.repository.remote_rest.response.OsmStationCandidate
import com.z_company.repository.remote_rest.response.RouteLengthRequest
import com.z_company.repository.remote_rest.response.RouteLengthResponse
import com.z_company.repository.remote_rest.response.ServicePhaseLookupRequest
import com.z_company.repository.remote_rest.response.ServicePhaseSuggestRequest
import com.z_company.domain.track.StationNameMatcher
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
/** Внутренний сигнал: гео-запросы на паузе после 429 (SCREEN_SPECS §6.9), на сервер не ходили. */
private class RateLimitPauseException : RuntimeException()

enum class AutoFillFailure { NO_INTERNET, NO_CONNECTION, VPN, STATION_NOT_FOUND, NO_PATH, SERVICE, RATE_LIMITED, BUSY }

/** Короткий текст сетевой/серверной ошибки для снекбаров и подписей. */
fun AutoFillFailure.message(): String = when (this) {
    AutoFillFailure.NO_INTERNET -> "Нет подключения к интернету"
    AutoFillFailure.NO_CONNECTION -> "Не удалось связаться с сервером — проверьте подключение к интернету"
    AutoFillFailure.VPN -> com.z_company.core.util.VPN_ERROR_HINT
    AutoFillFailure.RATE_LIMITED -> "Слишком много запросов. Попробуйте позже или войдите в аккаунт"
    AutoFillFailure.BUSY -> "Сервер занят или долго считал — повторите через минуту"
    else -> "Сервис не ответил — попробуйте позже"
}

/** Отказ по HTTP-статусу из текста исключения: 429 — лимит, 503 — «сервис занят / карты недоступны» (повторить). */
internal fun httpFailureOf(text: String): AutoFillFailure? = when {
    text.contains("429") -> AutoFillFailure.RATE_LIMITED
    text.contains("503") || text.contains("Service Unavailable", ignoreCase = true) -> AutoFillFailure.BUSY
    else -> null
}

/**
 * Ответ сервера «путь не найден» (404 с описанием, [route/length]) - а не отсутствие эндпоинта у старого сервера
 * (его тело - `{"detail":"Not Found"}`). Ktor пишет статус как «404 Not Found», поэтому искать в тексте нужно
 * кавычки вокруг «Not Found» из JSON-тела, а не саму фразу: иначе настоящее «путь не найден» выглядело как «сервис не ответил».
 */
internal fun isNoPathHttpError(message: String): Boolean =
    message.contains("404") && !message.contains("\"Not Found\"")

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

    /** Между крайними станциями несколько путей — сначала выбор маршрута (экран вариантов), потом подбор станций по нему. */
    data class ChooseRoute(val variants: List<RouteVariant>) : AutoFillState

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
        /** Станции плеча с координатами по порядку: путь автоподбора — строго через них. */
        viaPoints: List<Pair<Double, Double>> = emptyList(),
        /** Нет промежуточных станций — сначала предложить выбрать маршрут, если путей несколько. */
        pickRoute: Boolean = false,
    ) {
        request = ServicePhaseSuggestRequest(
            departureStation = departure.trim(),
            arrivalStation = arrival.trim(),
            country = country.ifBlank { "RU" },
            departureOsmId = departureOsmId,
            arrivalOsmId = arrivalOsmId,
            departurePoint = departurePoint?.let { (lat, lon) -> LatLon(lat, lon) },
            arrivalPoint = arrivalPoint?.let { (lat, lon) -> LatLon(lat, lon) },
            viaPoints = viaPoints.map { (lat, lon) -> LatLon(lat, lon) },
        )
        if (pickRoute && viaPoints.isEmpty() && departurePoint != null && arrivalPoint != null) {
            // Варианты пути между крайними станциями: больше одного — выбор маршрута, иначе сразу подбор.
            job?.cancel()
            job = viewModelScope.launch {
                _state.value = AutoFillState.Loading(0)
                val res = measure(listOf(departurePoint, arrivalPoint), withVariants = true) as? MeasureResult.Ok
                val variants = res?.variants.orEmpty()
                if (variants.size > 1) _state.value = AutoFillState.ChooseRoute(variants)
                else run()
            }
            return
        }
        run()
    }

    /** После выбора маршрута: станции подбираются вдоль него ([vias] — его точки по порядку; пусто — кратчайший). */
    fun chooseRoute(vias: List<Pair<Double, Double>>) {
        val r = request ?: return
        request = r.copy(viaPoints = vias.map { (lat, lon) -> LatLon(lat, lon) })
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
        if (GeoRateLimit.remainingMs() > 0) {
            _state.value = AutoFillState.Failed(AutoFillFailure.RATE_LIMITED)
            return
        }
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
                val token = bearerOrNull()
                withTimeout(TIMEOUT_MS) { api.suggestServicePhaseStations(token, r) }
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
                    end, result.name ?: "", markSimilarCandidates(result.name.orEmpty(), result.candidates),
                    approximate = result.approximate || result.candidates.isNotEmpty() && result.candidates.all {
                        it.approximate || isLocalSimilarName(result.name.orEmpty(), it.name)
                    },
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
    ): MeasureResult =
        if (GeoRateLimit.remainingMs() > 0) MeasureResult.Failed(AutoFillFailure.RATE_LIMITED)
        else measureRequest(points, withVariants, withGeometry)

    private suspend fun measureRequest(
        points: List<Pair<Double, Double>>,
        withVariants: Boolean,
        withGeometry: Boolean,
    ): MeasureResult = try {
        val token = bearerOrNull()
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
                val canonical = api.measureRouteLength(token, canonicalRequest)
                if (requestedIsCanonical) canonical else reverseRouteResponse(canonical)
            } else {
                api.measureRouteLength(token, request)
            }
        }
        if (withGeometry) saveRailLine(points, res.geometry)
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
        if (isNoPathHttpError(msg)) MeasureResult.NoPath
        else MeasureResult.Failed(failureOf(e))
    }

    /**
     * Причина сбоя запроса: 429 — лимит; сетевая ошибка — нет интернета / нет связи с сервером /
     * VPN (по состоянию сети телефона); иначе — ошибка сервиса. Таймаут 30 с при живом
     * интернете — «сервис не ответил».
     */
    private fun failureOf(e: Throwable): AutoFillFailure {
        val chain = generateSequence(e) { it.cause }.take(6).toList()
        GeoRateLimit.recordIfRateLimited(chain)
        android.util.Log.w("ServicePhaseNet", "request failed: " + chain.joinToString(" <- ") { it::class.simpleName.orEmpty() })
        val text = chain.joinToString(" ") { "${it::class.simpleName} ${it.message.orEmpty()}" }
        httpFailureOf(text)?.let { return it }
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

    /** Токен или null: без аккаунта гео-запросы идут анонимно (лимит по IP, SCREEN_SPECS §6.9). */
    private suspend fun bearerOrNull(): String? =
        tokens.getAuthBearerTokenFlow().first()?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

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
    private val railCache by lazy { com.z_company.route.component.servicephase.RailLineCache(appContext) }

    private fun railCacheKey(points: List<Pair<Double, Double>>) = railCache.key(points)

    private fun railCacheFile(key: String) = railCache.file(key)

    /** Линия, полученная онлайн при замере/просмотре плеча, — запас для «Карты» без сети (тот же кэш, что у [railLine]). */
    private suspend fun saveRailLine(points: List<Pair<Double, Double>>, geometry: List<List<Double>>) {
        val line = geometry.mapNotNull { p -> if (p.size >= 2) p[0] to p[1] else null }
        if (line.size < 2) return
        val key = railCacheKey(points)
        val file = railCacheFile(key)
        withContext(Dispatchers.IO) {
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(key + "\n" + line.joinToString(";") { (lat, lon) -> "$lat,$lon" })
            }
        }
    }

    suspend fun railLine(points: List<Pair<Double, Double>>): RailLineResult {
        val key = railCacheKey(points)
        val file = railCacheFile(key)
        // Всегда сначала сервер: граф путей обновляется, и сохранённая линия могла устареть
        // (так было после добавления станционных путей — карта рисовала старый объезд).
        // Кэш — только запас, когда сервер недоступен.
        var result: RailLineResult = RailLineResult.Failed(AutoFillFailure.SERVICE)
        for (attempt in 0..1) {
            if (attempt > 0) delay(2_000)
            result = try {
                if (GeoRateLimit.remainingMs() > 0) {
                    result = RailLineResult.Failed(AutoFillFailure.RATE_LIMITED)
                    break
                }
                val token = bearerOrNull()
                val line = withTimeout(TIMEOUT_MS) {
                    api.measureRouteLength(
                        token,
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
                if (isNoPathHttpError(msg)) RailLineResult.NoPath
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

    private fun readRailLine(file: java.io.File, key: String): List<Pair<Double, Double>>? = railCache.read(file, key)

    /** Координаты одной станции (шторка станции, «Найти координаты»). null — ошибка сети/сервиса. */
    suspend fun lookup(
        name: String,
        country: String,
        near: List<Pair<Double, Double>>,
        limit: Int? = null,
    ): List<OsmStationCandidate>? =
        try {
            lastLookupFailure = null
            if (GeoRateLimit.remainingMs() > 0) throw RateLimitPauseException()
            val token = bearerOrNull()
            withTimeout(TIMEOUT_MS) {
                api.lookupServicePhaseStation(
                    token,
                    ServicePhaseLookupRequest(
                        name = name.trim(),
                        country = country.ifBlank { "RU" },
                        near = near.map { (lat, lon) -> LatLon(lat, lon) },
                        limit = limit,
                    ),
                ).candidates.let { markSimilarCandidates(name, it) }
            }
        } catch (e: RateLimitPauseException) {
            lastLookupFailure = AutoFillFailure.RATE_LIMITED
            null
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

    /** Локально помечаем уже полученные варианты сокращения; отдельный поиск и второй список не создаём. */
    private fun markSimilarCandidates(query: String, candidates: List<OsmStationCandidate>) =
        candidates.map { candidate ->
            if (candidate.approximate || isLocalSimilarName(query, candidate.name)) candidate.copy(approximate = true)
            else candidate
        }

    private fun isLocalSimilarName(query: String, candidate: String): Boolean {
        fun key(value: String) = value.lowercase().replace('ё', 'е').filter { it.isLetterOrDigit() }
        return key(query) != key(candidate) && StationNameMatcher.sameStation(query, candidate)
    }

    private companion object {
        const val MAX_VARIANTS = 3
        const val TIMEOUT_MS = 30_000L
        /** Сетевые исключения Ktor/JVM, которые не наследуют IOException. */
        val NETWORK_MARKERS = listOf("UnresolvedAddress", "UnknownHost", "ConnectException", "NoRouteToHost",
            "SocketTimeout", "ConnectTimeout", "HttpRequestTimeout", "SSLException", "ClosedReceiveChannel")
    }
}
