package com.z_company.domain.util

import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhasePoint
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.entities.setting.ServicePhaseStationKind

/**
 * Справочник станций (StationNorm) — единый источник координат станций (SCREEN_SPECS §6.6).
 *
 * Связь с плечами — по названию ([ServicePhaseStationsMerge.normalizeName]). Координаты в
 * самом плече — запасная копия (для старых клиентов/PWA): при чтении плеча справочник
 * побеждает ([withDirectory]), при сохранении плеча его координаты пишутся в справочник
 * ([mergePhase]). Так правка в карточке станции сразу видна во всех плечах.
 */
object StationDirectory {

    private fun key(name: String) = ServicePhaseStationsMerge.normalizeName(name)

    /** Станции справочника с координатами по нормализованному названию. */
    fun byName(stations: List<StationNorm>): Map<String, StationNorm> =
        stations.filter { it.hasCoordinates && it.name.isNotBlank() }.associateBy { key(it.name) }

    /** Плечо с координатами станций из справочника (где они там есть). */
    fun ServicePhase.withDirectory(stations: List<StationNorm>): ServicePhase {
        val known = byName(stations)
        if (known.isEmpty()) return this
        fun point(name: String, own: ServicePhasePoint?): ServicePhasePoint? {
            val st = known[key(name)] ?: return own
            return ServicePhasePoint(
                latitude = st.latitude!!,
                longitude = st.longitude!!,
                osmId = st.osmId ?: own?.osmId,
                esr = st.esr ?: own?.esr,
            )
        }
        return copy(
            departurePoint = point(departureStation, departurePoint),
            arrivalPoint = point(arrivalStation, arrivalPoint),
            intermediateStations = intermediateStations.map { it.withDirectory(known) },
        )
    }

    private fun ServicePhaseStation.withDirectory(known: Map<String, StationNorm>): ServicePhaseStation {
        val st = known[key(name)] ?: return this
        return copy(
            latitude = st.latitude,
            longitude = st.longitude,
            esr = st.esr ?: esr,
            osmId = st.osmId ?: osmId,
            kind = st.kind ?: kind,
        )
    }

    /** Координаты станций плеча (крайние и промежуточные), которые стоит записать в справочник. */
    private data class Found(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val esr: String?,
        val osmId: Long?,
        val kind: ServicePhaseStationKind?,
    )

    private fun found(phase: ServicePhase): List<Found> = buildList {
        phase.departurePoint?.let {
            add(Found(phase.departureStation, it.latitude, it.longitude, it.esr, it.osmId, null))
        }
        phase.intermediateStations.forEach { st ->
            if (st.latitude != null && st.longitude != null) {
                add(Found(st.name, st.latitude, st.longitude, st.esr, st.osmId, st.kind))
            }
        }
        phase.arrivalPoint?.let {
            add(Found(phase.arrivalStation, it.latitude, it.longitude, it.esr, it.osmId, null))
        }
    }.filter { it.name.isNotBlank() }

    /**
     * Справочник после сохранения плеча: станции с координатами из плеча создаются или
     * обновляются (координаты, ЕСР, OSM, тип; нормы и км оси не трогаем).
     * [overwrite] = false — только дописать координаты станциям, у которых их нет
     * (перенос из плеч, сохранённых до появления справочника).
     * null — справочник не изменился.
     */
    fun mergePhase(
        stations: List<StationNorm>,
        phase: ServicePhase,
        now: Long,
        overwrite: Boolean = true,
    ): List<StationNorm>? {
        val out = stations.toMutableList()
        var changed = false
        found(phase).distinctBy { key(it.name) }.forEach { f ->
            val idx = out.indexOfFirst { key(it.name) == key(f.name) }
            if (idx < 0) {
                out += StationNorm(
                    name = f.name.trim(),
                    latitude = f.latitude,
                    longitude = f.longitude,
                    esr = f.esr,
                    osmId = f.osmId,
                    kind = f.kind,
                    updatedAt = now,
                )
                changed = true
            } else if (overwrite || !out[idx].hasCoordinates) {
                val old = out[idx]
                val upd = old.copy(
                    latitude = f.latitude,
                    longitude = f.longitude,
                    esr = f.esr ?: old.esr,
                    osmId = f.osmId ?: old.osmId,
                    kind = f.kind ?: old.kind,
                )
                if (upd != old) {
                    out[idx] = upd.copy(updatedAt = now)
                    changed = true
                }
            }
        }
        return out.takeIf { changed }
    }

    /** [mergePhase] по всем плечам без перезаписи — перенос координат из старых плеч. */
    fun backfill(stations: List<StationNorm>, phases: List<ServicePhase>, now: Long): List<StationNorm>? {
        var current = stations
        var changed = false
        phases.forEach { phase ->
            mergePhase(current, phase, now, overwrite = false)?.let {
                current = it
                changed = true
            }
        }
        return current.takeIf { changed }
    }

    /**
     * Ориентиры для станции [name] (поиск координат, карта): координаты соседних станций
     * по плечам, где она встречается, иначе — станции этих плеч вообще.
     */
    fun neighbours(name: String, phases: List<ServicePhase>, stations: List<StationNorm>): List<Pair<Double, Double>> {
        val k = key(name)
        val out = mutableListOf<Pair<Double, Double>>()
        phases.map { it.withDirectory(stations) }.forEach { phase ->
            val stops = buildList {
                add(phase.departureStation to phase.departurePoint?.let { it.latitude to it.longitude })
                phase.intermediateStations.forEach { st ->
                    add(st.name to st.latitude?.let { lat -> st.longitude?.let { lat to it } })
                }
                add(phase.arrivalStation to phase.arrivalPoint?.let { it.latitude to it.longitude })
            }
            val idx = stops.indexOfFirst { key(it.first) == k }
            if (idx < 0) return@forEach
            listOfNotNull(stops.getOrNull(idx - 1)?.second, stops.getOrNull(idx + 1)?.second).forEach(out::add)
        }
        return out.distinct().take(10)
    }

    /** Координаты станций справочника по нормализованному названию (подсказки/GPS). */
    fun points(stations: List<StationNorm>): Map<String, Pair<Double, Double>> =
        byName(stations).mapValues { (_, st) -> st.latitude!! to st.longitude!! }

    /** Нормализованное название → название как в справочнике (для станций с координатами). */
    fun names(stations: List<StationNorm>): Map<String, String> =
        byName(stations).mapValues { (_, st) -> st.name.trim() }
}
