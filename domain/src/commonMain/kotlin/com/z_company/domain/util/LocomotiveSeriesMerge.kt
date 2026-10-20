package com.z_company.domain.util

import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.LocomotiveSeriesTombstone

/**
 * Слияние справочника серий между устройствами — те же правила, что у [StationNormMerge]:
 * last-write-wins по каждой серии, удаление — тоже запись (при равенстве побеждает оно),
 * серии с одинаковыми названием и типом схлопываются в одну. Маршруты ссылаются на серию по
 * названию, поэтому перенаправлять ссылки не нужно.
 */
object LocomotiveSeriesMerge {
    data class Result(
        val series: List<LocomotiveSeries>,
        val tombstones: List<LocomotiveSeriesTombstone>,
    )

    fun merge(
        localSeries: List<LocomotiveSeries>,
        localTombstones: List<LocomotiveSeriesTombstone>,
        remoteSeries: List<LocomotiveSeries>,
        remoteTombstones: List<LocomotiveSeriesTombstone>,
    ): Result {
        val series = HashMap<String, LocomotiveSeries>()
        val tombstones = HashMap<String, LocomotiveSeriesTombstone>()

        fun offerSeries(s: LocomotiveSeries) {
            val dead = tombstones[s.seriesId]
            if (dead != null && dead.deletedAt >= s.updatedAt) return
            val known = series[s.seriesId]
            if (known != null && known.updatedAt >= s.updatedAt) {
                if (known.brakeFieldsEmpty() && !s.brakeFieldsEmpty()) series[s.seriesId] = known.withBrakeFieldsOf(s)
                return
            }
            series[s.seriesId] = if (known != null && s.brakeFieldsEmpty()) s.withBrakeFieldsOf(known) else s
            if (dead != null) tombstones.remove(s.seriesId)
        }

        fun offerTombstone(t: LocomotiveSeriesTombstone) {
            val known = tombstones[t.seriesId]
            if (known != null && known.deletedAt >= t.deletedAt) return
            val alive = series[t.seriesId]
            if (alive != null && alive.updatedAt > t.deletedAt) return
            tombstones[t.seriesId] = t
            if (alive != null) series.remove(t.seriesId)
        }

        localSeries.forEach(::offerSeries)
        remoteSeries.forEach(::offerSeries)
        localTombstones.forEach(::offerTombstone)
        remoteTombstones.forEach(::offerTombstone)

        dedupe(series, tombstones)

        return Result(
            series = series.values.sortedBy { it.name },
            tombstones = tombstones.values.sortedBy { it.seriesId },
        )
    }

    private fun filled(s: LocomotiveSeries): Int = listOf(
        s.acceptanceDurationMin, s.deliveryDurationMin, s.acceptanceHandToHandMin, s.deliveryHandToHandMin,
        s.brakeVolumeL, s.axleCount, s.axleLoadTf, s.weightT, s.sectionCount,
    ).count { it != null }

    /**
     * Паспортные поля (объём ГР, оси, нажатие, вес, секции) пусты целиком: запись правил старый клиент, который их не
     * знает (сервер их сохраняет, но свежая копия от старого клиента может прийти без них) - тогда берём их у другой копии.
     */
    private fun LocomotiveSeries.brakeFieldsEmpty(): Boolean =
        brakeVolumeL == null && axleCount == null && axleLoadTf == null && weightT == null && sectionCount == null

    private fun LocomotiveSeries.withBrakeFieldsOf(other: LocomotiveSeries): LocomotiveSeries = copy(
        brakeVolumeL = other.brakeVolumeL,
        axleCount = other.axleCount,
        axleLoadTf = other.axleLoadTf,
        weightT = other.weightT,
        sectionCount = other.sectionCount,
    )

    private fun dedupe(
        series: MutableMap<String, LocomotiveSeries>,
        tombstones: MutableMap<String, LocomotiveSeriesTombstone>,
    ) {
        series.values.groupBy { ServicePhaseStationsMerge.normalizeName(it.name) to it.type }
            .values.filter { it.size > 1 }
            .forEach { group ->
                val ordered = group.sortedWith(
                    compareByDescending<LocomotiveSeries> { filled(it) }
                        .thenByDescending { it.updatedAt }
                        .thenBy { it.seriesId }
                )
                val winner = ordered.first()
                val stamp = group.maxOf { it.updatedAt } + 1
                var merged = winner
                for (l in ordered.drop(1)) {
                    merged = merged.copy(
                        acceptanceDurationMin = merged.acceptanceDurationMin ?: l.acceptanceDurationMin,
                        deliveryDurationMin = merged.deliveryDurationMin ?: l.deliveryDurationMin,
                        acceptanceHandToHandMin = merged.acceptanceHandToHandMin ?: l.acceptanceHandToHandMin,
                        deliveryHandToHandMin = merged.deliveryHandToHandMin ?: l.deliveryHandToHandMin,
                        brakeVolumeL = merged.brakeVolumeL ?: l.brakeVolumeL,
                        axleCount = merged.axleCount ?: l.axleCount,
                        axleLoadTf = merged.axleLoadTf ?: l.axleLoadTf,
                        weightT = merged.weightT ?: l.weightT,
                        sectionCount = merged.sectionCount ?: l.sectionCount,
                    )
                    series.remove(l.seriesId)
                    tombstones[l.seriesId] = LocomotiveSeriesTombstone(l.seriesId, l.name, l.type, stamp)
                }
                if (merged != winner) merged = merged.copy(updatedAt = stamp)
                series[winner.seriesId] = merged
            }
    }
}
