package com.z_company.domain.util

import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.norma_time.StationTombstone

/**
 * Слияние справочника станций между устройствами (last-write-wins по каждой станции).
 * Для каждого `stationId` побеждает запись с большим временем: `updatedAt` у живой станции,
 * `deletedAt` у надгробия. При равенстве побеждает удаление.
 *
 * Станции с одинаковым названием (разные id — добавлены на разных устройствах независимо)
 * схлопываются в одну: остаётся «самая полная», а недостающие у неё поля берутся у
 * остальных; лишние id становятся надгробиями. Выбор зависит только от набора данных, а
 * не от порядка, поэтому все устройства приходят к одному результату.
 */
object StationNormMerge {
    data class Result(
        val stations: List<StationNorm>,
        val tombstones: List<StationTombstone>,
    )

    fun merge(
        localStations: List<StationNorm>,
        localTombstones: List<StationTombstone>,
        remoteStations: List<StationNorm>,
        remoteTombstones: List<StationTombstone>,
    ): Result {
        val stations = HashMap<String, StationNorm>()
        val tombstones = HashMap<String, StationTombstone>()

        fun offerStation(s: StationNorm) {
            val dead = tombstones[s.stationId]
            if (dead != null && dead.deletedAt >= s.updatedAt) return
            val known = stations[s.stationId]
            if (known != null && known.updatedAt >= s.updatedAt) return
            stations[s.stationId] = s
            if (dead != null) tombstones.remove(s.stationId)
        }

        fun offerTombstone(t: StationTombstone) {
            val known = tombstones[t.stationId]
            if (known != null && known.deletedAt >= t.deletedAt) return
            val alive = stations[t.stationId]
            if (alive != null && alive.updatedAt > t.deletedAt) return
            tombstones[t.stationId] = t
            if (alive != null) stations.remove(t.stationId)
        }

        localStations.forEach(::offerStation)
        remoteStations.forEach(::offerStation)
        localTombstones.forEach(::offerTombstone)
        remoteTombstones.forEach(::offerTombstone)

        dedupeByName(stations, tombstones)

        return Result(
            stations = stations.values.sortedBy { it.name },
            tombstones = tombstones.values.sortedBy { it.stationId },
        )
    }

    private fun filled(s: StationNorm): Int = listOf(
        s.appearanceToStartMin, s.endToBarrierMin, s.barrierToStartMin, s.endToWorkEndMin,
        s.latitude, s.esr, s.axisKm,
    ).count { it != null }

    private fun dedupeByName(
        stations: MutableMap<String, StationNorm>,
        tombstones: MutableMap<String, StationTombstone>,
    ) {
        stations.values.groupBy { ServicePhaseStationsMerge.normalizeName(it.name) }
            .values.filter { it.size > 1 }
            .forEach { group ->
                val ordered = group.sortedWith(
                    compareByDescending<StationNorm> { filled(it) }
                        .thenByDescending { it.updatedAt }
                        .thenBy { it.stationId }
                )
                val winner = ordered.first()
                val losers = ordered.drop(1)
                val stamp = group.maxOf { it.updatedAt } + 1
                var merged = winner
                for (l in losers) {
                    merged = merged.copy(
                        appearanceToStartMin = merged.appearanceToStartMin ?: l.appearanceToStartMin,
                        endToBarrierMin = merged.endToBarrierMin ?: l.endToBarrierMin,
                        barrierToStartMin = merged.barrierToStartMin ?: l.barrierToStartMin,
                        endToWorkEndMin = merged.endToWorkEndMin ?: l.endToWorkEndMin,
                        esr = merged.esr ?: l.esr,
                        osmId = merged.osmId ?: l.osmId,
                        kind = merged.kind ?: l.kind,
                        axisKm = merged.axisKm ?: l.axisKm,
                        axisPicket = merged.axisPicket ?: l.axisPicket,
                    )
                    if (merged.latitude == null && merged.longitude == null) {
                        merged = merged.copy(latitude = l.latitude, longitude = l.longitude)
                    }
                    stations.remove(l.stationId)
                    tombstones[l.stationId] = StationTombstone(l.stationId, l.name, stamp)
                }
                if (merged != winner) merged = merged.copy(updatedAt = stamp)
                stations[winner.stationId] = merged
            }
    }

    /**
     * Куда перенаправить ссылки на удалённые станции: id надгробия → id живой станции с тем
     * же названием (так после схлопывания дублей маршруты не теряют станцию).
     */
    fun redirects(stations: List<StationNorm>, tombstones: List<StationTombstone>): Map<String, String> {
        val byName = stations.associateBy { ServicePhaseStationsMerge.normalizeName(it.name) }
        return tombstones.mapNotNull { t ->
            byName[ServicePhaseStationsMerge.normalizeName(t.name)]?.let { t.stationId to it.stationId }
        }.toMap()
    }
}
