package com.z_company.domain.util

import com.z_company.domain.autorecord.StopDetectionConfig
import com.z_company.domain.entities.setting.ServicePhase

/** Станция, найденная сервером по рельсам (`stations/nearest`, `alongRail=true`). */
data class RailStation(
    /** null у выбранного кандидата клиента (станции справочника). */
    val osmId: Long?,
    val name: String,
    val kind: String,
    val esr: String?,
    val latitude: Double,
    val longitude: Double,
    /** Расстояние до станции по пути, м. */
    val distanceAlongM: Int?,
    /** Расстояние точки до пути, м. */
    val offsetM: Int?,
    /** Не null, если сервер выбрал станцию справочника клиента (`source=candidate`): имя берём из справочника по ключу. */
    val candidateKey: String? = null,
)

/** Станция справочника клиента, отправляемая серверу как кандидат (только с координатами). */
data class RailCandidate(val key: String, val name: String, val latitude: Double, val longitude: Double)

/** Ответ поиска ближайшей станции по рельсам. */
sealed interface RailLookupResult {
    /** Станция найдена. */
    data class Found(val station: RailStation) : RailLookupResult

    /** Сервер ответил, но станции нет: `off_rails`, `not_found`, `no_rail_data` (или без причины). */
    data class NoStation(val reason: String?) : RailLookupResult

    /** Ответа нет: нет сети, ошибка, 503 (сервис занят; [retryAfterMs] из Retry-After). Не кэшируется. */
    data class Unavailable(val retryAfterMs: Long? = null) : RailLookupResult
}

/** Источник «ближайшая станция по рельсам» (сервер; в тестах - фейк). Координата только в запросе, не логируется. */
fun interface RailStationLookup {
    suspend fun nearestAlongRail(
        latitude: Double,
        longitude: Double,
        radiusM: Double,
        candidates: List<RailCandidate>,
    ): RailLookupResult
}

/** Источник рельсовой линии плеча (как на карте поезда; кэш устройства). null - линии нет. Вызывается вне главного потока. */
fun interface PhaseLineSource {
    suspend fun line(stationPoints: List<Pair<Double, Double>>): List<Pair<Double, Double>>?
}

/**
 * Кэш последнего определённого ответа на устройстве (в памяти, не сохраняется): не спрашиваем сервер повторно,
 * пока позиция сдвинулась меньше [MOVE_M] и прошло меньше [TTL_MS].
 */
class RailLookupCache(private val nowMs: () -> Long) {
    private var lat = 0.0
    private var lon = 0.0
    private var at = 0L
    private var result: RailLookupResult? = null

    fun get(latitude: Double, longitude: Double): RailLookupResult? {
        val r = result ?: return null
        if (nowMs() - at >= TTL_MS) return null
        return r.takeIf { ServicePhaseStationPicker.distanceM(lat, lon, latitude, longitude) < MOVE_M }
    }

    fun put(latitude: Double, longitude: Double, value: RailLookupResult) {
        if (value is RailLookupResult.Unavailable) return
        lat = latitude; lon = longitude; at = nowMs(); result = value
    }

    companion object {
        const val MOVE_M = 150.0
        const val TTL_MS = 2 * 60_000L
    }
}

/**
 * ЕДИНОЕ определение названия станции по месту (GO, «Добавить станцию», АВТО, карта поезда, просмотр трека;
 * SCREEN_SPECS §6.2 п.6/6а, §6.9):
 * 1. выбрано плечо с координатами станций - [ServicePhaseStationPicker.pickForStop] (радиус 1 км; кандидаты - станции плеча
 *    и станции справочника на линии плеча [phaseLine]; порядок вдоль линии, иначе справочник, иначе null);
 * 2. плеча нет (или у него нет координат) - ближайшая станция ПО РЕЛЬСАМ с сервера (радиус [StopDetectionConfig.STATION_RADIUS_M]);
 *    вместе с точкой уходят кандидаты - станции справочника в [CANDIDATE_RADIUS_M] (<= [MAX_CANDIDATES], ближайшие по прямой);
 *    если сервер выбрал кандидата - берём имя справочника (в справочник ничего не добавляем), если OSM-станцию - как раньше;
 * 3. сервер не ответил / `off_rails` / `not_found` / `no_rail_data` / нет сети - запасной вариант: ближайшая
 *    станция справочника по прямой в радиусе 1 км (как [ServicePhaseStationPicker.pickForStop] без плеча).
 */
class StopStationNamer(
    private val lookup: RailStationLookup?,
    private val nowMs: () -> Long,
    private val cache: RailLookupCache = RailLookupCache(nowMs),
    private val phaseLine: PhaseLineSource? = null,
) {
    private var unavailableUntil = 0L

    /**
     * @param onDiscovered вызывается, когда название взято у сервера: станцию можно дописать в справочник
     * (без перезаписи существующих).
     */
    suspend fun name(
        phase: ServicePhase?,
        trainStationNames: List<String?>,
        latitude: Double,
        longitude: Double,
        points: Map<String, Pair<Double, Double>>,
        names: Map<String, String>,
        onDiscovered: (suspend (RailStation) -> Unit)? = null,
    ): String? {
        val planned = phase?.takeIf(ServicePhaseStationPicker::hasCoordinates)
        if (planned == null) {
            val found = railStation(latitude, longitude, candidatesFor(latitude, longitude, points, names))
            if (found != null) {
                val key = found.candidateKey
                if (key != null) return names[key]?.takeIf { it.isNotBlank() } ?: found.name
                runCatching { onDiscovered?.invoke(found) }
                return found.name
            }
        }
        val line = planned?.let { p ->
            runCatching { phaseLine?.line(TrainMapRoute.stops(p).map { it.latitude to it.longitude }) }.getOrNull()
        }
        return ServicePhaseStationPicker.pickForStop(
            planned, trainStationNames, latitude, longitude, points, names, railLine = line,
        )
    }

    /**
     * Ближайшая станция к точке, на которую нажали на карте редактора плеча (SCREEN_SPECS §6.6): без плеча и без кэша -
     * каждая точка отдельный запрос. Сервер по рельсам вместе с кандидатами-справочником ([candidatesFor], радиус
     * [PointStationPicker.RADIUS_M]); сервер не ответил / `off_rails` / `not_found` - ближайшая станция справочника по
     * прямой, затем станция OSM, нарисованная на карте ([rendered]); иначе станции нет.
     */
    suspend fun stationAtPoint(
        latitude: Double,
        longitude: Double,
        points: Map<String, Pair<Double, Double>>,
        names: Map<String, String>,
        rendered: RenderedStation? = null,
    ): PointStationResult {
        val source = lookup
        val result = if (source == null) RailLookupResult.Unavailable() else runCatching {
            source.nearestAlongRail(latitude, longitude, PointStationPicker.RADIUS_M, candidatesFor(latitude, longitude, points, names))
        }.getOrElse { RailLookupResult.Unavailable() }
        val offline = result is RailLookupResult.Unavailable
        val found = (result as? RailLookupResult.Found)?.station?.takeIf { it.name.isNotBlank() }
        if (found != null) {
            return PointStationResult(PointStationPicker.fromRail(found, latitude, longitude, points, names), offline = false)
        }
        PointStationPicker.nearestDirectory(latitude, longitude, points, names)?.let { return PointStationResult(it, offline) }
        rendered?.takeIf { it.name.isNotBlank() }?.let {
            val st = PointStationPicker.fromRendered(it, latitude, longitude, points, names)
            if (st.distanceM <= PointStationPicker.RADIUS_M) return PointStationResult(st, offline)
        }
        return PointStationResult(null, offline)
    }

    private suspend fun railStation(latitude: Double, longitude: Double, candidates: List<RailCandidate>): RailStation? {
        val source = lookup ?: return null
        val result = cache.get(latitude, longitude) ?: run {
            if (nowMs() < unavailableUntil) return null
            val fresh = runCatching {
                source.nearestAlongRail(latitude, longitude, StopDetectionConfig.STATION_RADIUS_M, candidates)
            }.getOrElse { RailLookupResult.Unavailable() }
            if (fresh is RailLookupResult.Unavailable) {
                unavailableUntil = nowMs() + (fresh.retryAfterMs ?: DEFAULT_BACKOFF_MS)
            } else cache.put(latitude, longitude, fresh)
            fresh
        }
        return (result as? RailLookupResult.Found)?.station?.takeIf { it.name.isNotBlank() }
    }

    companion object {
        const val CANDIDATE_RADIUS_M = 2_000.0
        const val MAX_CANDIDATES = 40
        private const val MAX_KEY = 64
        private const val MAX_NAME = 120

        /** Станции справочника с координатами в [CANDIDATE_RADIUS_M], ближайшие по прямой, не более [MAX_CANDIDATES]. */
        fun candidatesFor(
            latitude: Double,
            longitude: Double,
            points: Map<String, Pair<Double, Double>>,
            names: Map<String, String>,
        ): List<RailCandidate> = points.asSequence()
            .mapNotNull { (key, p) ->
                val name = names[key]?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME } ?: return@mapNotNull null
                if (key.isEmpty() || key.length > MAX_KEY) return@mapNotNull null
                val d = ServicePhaseStationPicker.distanceM(latitude, longitude, p.first, p.second)
                if (d > CANDIDATE_RADIUS_M) null else d to RailCandidate(key, name, p.first, p.second)
            }
            .sortedBy { it.first }
            .take(MAX_CANDIDATES)
            .map { it.second }
            .toList()

        /** Пауза после недоступности сервера (Retry-After: 30), чтобы не долбить пачкой остановок. */
        const val DEFAULT_BACKOFF_MS = 30_000L
    }
}
