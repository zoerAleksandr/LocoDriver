package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.response.LatLon
import com.z_company.repository.remote_rest.response.OsmPathStation
import com.z_company.repository.remote_rest.response.OsmStationCandidate
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

enum class AutoFillFailure { NO_INTERNET, STATION_NOT_FOUND, NO_PATH, SERVICE, RATE_LIMITED }

sealed interface AutoFillState {
    data object Idle : AutoFillState

    /** step: 0 — ищем крайние станции, 1 — прокладываем путь, 2 — собираем станции. */
    data class Loading(val step: Int) : AutoFillState

    data class Choose(val end: PhaseEnd, val query: String, val candidates: List<OsmStationCandidate>) : AutoFillState

    data class Ready(
        val departure: OsmStationCandidate,
        val arrival: OsmStationCandidate,
        val lengthKm: Double,
        val stations: List<OsmPathStation>,
    ) : AutoFillState

    data class Failed(val failure: AutoFillFailure, val end: PhaseEnd? = null, val name: String? = null) : AutoFillState
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
    ) {
        request = ServicePhaseSuggestRequest(
            departureStation = departure.trim(),
            arrivalStation = arrival.trim(),
            country = country.ifBlank { "RU" },
            departureOsmId = departureOsmId,
            arrivalOsmId = arrivalOsmId,
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
            } catch (e: TimeoutCancellationException) {
                fail(ticker, AutoFillFailure.SERVICE)
                return@launch
            } catch (e: IOException) {
                // Таймаут Ktor — тоже IOException, но это «сервис не ответил», а не нет связи.
                val timeout = e::class.simpleName.orEmpty().contains("Timeout")
                fail(ticker, if (timeout) AutoFillFailure.SERVICE else AutoFillFailure.NO_INTERNET)
                return@launch
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // 429 — сервер ограничивает частоту автоподбора; Ktor кладёт код в текст исключения.
                val limited = e.message.orEmpty().contains("429")
                fail(ticker, if (limited) AutoFillFailure.RATE_LIMITED else AutoFillFailure.SERVICE)
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
                "ambiguous" -> AutoFillState.Choose(end, result.name ?: "", result.candidates)
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
        data class Ok(val lengthKm: Double, val kmAt: List<Double>) : MeasureResult
        data object NoPath : MeasureResult
        data class Failed(val failure: AutoFillFailure) : MeasureResult
    }

    /** «Измерить по карте»: длина по ж/д путям через точки по порядку (отправление … прибытие). */
    suspend fun measure(points: List<Pair<Double, Double>>): MeasureResult = try {
        val token = tokens.getAuthBearerTokenFlow().first()
        val res = withTimeout(TIMEOUT_MS) {
            api.measureRouteLength(
                "Bearer $token",
                com.z_company.repository.remote_rest.response.RouteLengthRequest(points.map { (lat, lon) -> LatLon(lat, lon) }),
            )
        }
        MeasureResult.Ok(res.lengthKm, res.kmAt)
    } catch (e: TimeoutCancellationException) {
        MeasureResult.Failed(AutoFillFailure.SERVICE)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: IOException) {
        val timeout = e::class.simpleName.orEmpty().contains("Timeout")
        MeasureResult.Failed(if (timeout) AutoFillFailure.SERVICE else AutoFillFailure.NO_INTERNET)
    } catch (e: Exception) {
        val msg = e.message.orEmpty()
        when {
            // 404 «Not Found» — эндпоинта ещё нет на сервере; наш 404 — «путь не найден».
            msg.contains("404") && !msg.contains("Not Found") -> MeasureResult.NoPath
            msg.contains("429") -> MeasureResult.Failed(AutoFillFailure.RATE_LIMITED)
            else -> MeasureResult.Failed(AutoFillFailure.SERVICE)
        }
    }

    /**
     * Линия плеча по рельсам для карты (через точки по порядку). Однажды полученная линия
     * кэшируется на устройстве по набору точек — карта рисует её и без сети.
     * null — нет пути/сети/сервиса.
     */
    suspend fun railLine(points: List<Pair<Double, Double>>): List<Pair<Double, Double>>? {
        val key = points.joinToString(";") { (lat, lon) -> "%.5f,%.5f".format(java.util.Locale.US, lat, lon) }
        val file = java.io.File(appContext.noBackupFilesDir, "rail_lines/${key.hashCode().toUInt().toString(16)}.txt")
        withContext(Dispatchers.IO) { readRailLine(file, key) }?.let { return it }
        val line = try {
            val token = tokens.getAuthBearerTokenFlow().first()
            withTimeout(TIMEOUT_MS) {
                api.measureRouteLength(
                    "Bearer $token",
                    com.z_company.repository.remote_rest.response.RouteLengthRequest(
                        points = points.map { (lat, lon) -> LatLon(lat, lon) },
                        withGeometry = true,
                    ),
                ).geometry.mapNotNull { p -> if (p.size >= 2) p[0] to p[1] else null }.takeIf { it.size >= 2 }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (e is TimeoutCancellationException) null else throw e
        } catch (e: Exception) {
            null
        } ?: return null
        withContext(Dispatchers.IO) {
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(key + "\n" + line.joinToString(";") { (lat, lon) -> "$lat,$lon" })
            }
        }
        return line
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
            if (e is TimeoutCancellationException) null else throw e
        } catch (e: Exception) {
            null
        }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
