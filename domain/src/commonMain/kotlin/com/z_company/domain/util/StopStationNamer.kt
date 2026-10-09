package com.z_company.domain.util

import com.z_company.domain.autorecord.StopDetectionConfig
import com.z_company.domain.entities.setting.ServicePhase

/** Станция, найденная сервером по рельсам (`stations/nearest`, `alongRail=true`). */
data class RailStation(
    val osmId: Long,
    val name: String,
    val kind: String,
    val esr: String?,
    val latitude: Double,
    val longitude: Double,
    /** Расстояние до станции по пути, м. */
    val distanceAlongM: Int?,
    /** Расстояние точки до пути, м. */
    val offsetM: Int?,
)

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
    suspend fun nearestAlongRail(latitude: Double, longitude: Double, radiusM: Double): RailLookupResult
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
 * 1. выбрано плечо с координатами станций - [ServicePhaseStationPicker.pickForStop] (радиус 1 км, порядок плеча,
 *    иначе справочник, иначе null);
 * 2. плеча нет (или у него нет координат) - ближайшая станция ПО РЕЛЬСАМ с сервера (радиус [StopDetectionConfig.STATION_RADIUS_M]);
 * 3. сервер не ответил / `off_rails` / `not_found` / `no_rail_data` / нет сети - запасной вариант: ближайшая
 *    станция справочника по прямой в радиусе 1 км (как [ServicePhaseStationPicker.pickForStop] без плеча).
 */
class StopStationNamer(
    private val lookup: RailStationLookup?,
    private val nowMs: () -> Long,
    private val cache: RailLookupCache = RailLookupCache(nowMs),
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
            val found = railStation(latitude, longitude)
            if (found != null) {
                runCatching { onDiscovered?.invoke(found) }
                return found.name
            }
        }
        return ServicePhaseStationPicker.pickForStop(planned, trainStationNames, latitude, longitude, points, names)
    }

    private suspend fun railStation(latitude: Double, longitude: Double): RailStation? {
        val source = lookup ?: return null
        val result = cache.get(latitude, longitude) ?: run {
            if (nowMs() < unavailableUntil) return null
            val fresh = runCatching {
                source.nearestAlongRail(latitude, longitude, StopDetectionConfig.STATION_RADIUS_M)
            }.getOrElse { RailLookupResult.Unavailable() }
            if (fresh is RailLookupResult.Unavailable) {
                unavailableUntil = nowMs() + (fresh.retryAfterMs ?: DEFAULT_BACKOFF_MS)
            } else cache.put(latitude, longitude, fresh)
            fresh
        }
        return (result as? RailLookupResult.Found)?.station?.takeIf { it.name.isNotBlank() }
    }

    companion object {
        /** Пауза после недоступности сервера (Retry-After: 30), чтобы не долбить пачкой остановок. */
        const val DEFAULT_BACKOFF_MS = 30_000L
    }
}
