package com.z_company.domain.util

import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.norma_time.StationTombstone

/**
 * Слияние справочника станций между устройствами (last-write-wins по каждой станции).
 * Для каждого `stationId` побеждает запись с большим временем: `updatedAt` у живой станции,
 * `deletedAt` у надгробия. При равенстве побеждает удаление.
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

        return Result(
            stations = stations.values.sortedBy { it.name },
            tombstones = tombstones.values.sortedBy { it.stationId },
        )
    }
}
