package com.z_company.domain.track

import com.z_company.domain.util.GnssPlausibility

/**
 * Одноразовая починка уже испорченных границ треков (флаг `tracks_fixed_v2`): GNSS-спуфинг мог записать точку с ложной
 * меткой времени (сдвиг на недели/месяцы), из-за чего `startedAt`/`endedAt` и «время в пути» были неверными (9522 ч).
 * Точки с временем дальше [GnssPlausibility.TIME_OUTLIER_MS] от медианы трека отбрасываются, границы = min/max времени
 * оставшихся точек. Повторный запуск ничего не меняет.
 */
object TrackRepair {
    /** Возвращает число починенных треков. Треки, пишущиеся прямо сейчас (`endedAt >= liveFromMs`), не трогаются. */
    fun fixBounds(repository: TrackRepository, liveFromMs: Long = Long.MAX_VALUE): Int {
        var fixed = 0
        for (info in repository.tracks()) {
            val points = repository.points(info.trackId)
            if (points.isEmpty()) continue
            val clean = GnssPlausibility.dropTimeOutliers(points)
            if (clean.isEmpty()) continue
            val start = clean.first().timeMs
            val end = clean.last().timeMs
            if (clean.size == points.size && start == info.startedAt && end == info.endedAt) continue
            if (end >= liveFromMs) continue // пишется прямо сейчас (по честным точкам, а не по ложному endedAt)
            repository.trimTrack(info.trackId, start, end)
            fixed++
        }
        return fixed
    }
}
