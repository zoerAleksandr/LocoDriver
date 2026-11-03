package com.z_company.domain.track

import com.z_company.domain.entities.route.Train

/** Подготовка трека к отправке на сервер (SCREEN_SPECS §6.9): чанки, превью, сводка, заголовок. */
object TrackPacking {
    /** Точек в превью карточки (сервер принимает не больше 100). */
    const val PREVIEW_POINTS = 100

    /** Не отправлять трек, пока в нём не было новых точек дольше этого (сеанс «АВТО» ещё идёт). */
    const val QUIET_MS = 5 * 60_000L

    /** Последовательные чанки по [TrackWireCodec.MAX_CHUNK_POINTS] точек; порядок и границы детерминированы (идемпотентность). */
    fun chunks(points: List<TrackPoint>): List<List<TrackPoint>> =
        points.sortedBy { it.timeMs }.chunked(TrackWireCodec.MAX_CHUNK_POINTS)

    /** Равномерная выборка до [max] точек, первая и последняя сохраняются. */
    fun preview(points: List<TrackPoint>, max: Int = PREVIEW_POINTS): List<TrackPoint> {
        if (points.size <= max) return points
        val last = points.size - 1
        return List(max) { i -> points[(i.toLong() * last / (max - 1)).toInt()] }
    }

    data class Stats(val distanceKm: Double, val maxSpeedKmh: Double, val avgSpeedKmh: Double, val durationMs: Long)

    /** Длина по ломаной, максимальная и средняя (длина / время) скорость; пределы сервера: ≤ 500 км/ч, ≤ 20000 км. */
    fun stats(points: List<TrackPoint>): Stats {
        if (points.isEmpty()) return Stats(0.0, 0.0, 0.0, 0L)
        var meters = 0.0
        var maxSpeed = 0.0
        for (i in points.indices) {
            points[i].speedKmh?.let { if (it > maxSpeed) maxSpeed = it }
            if (i > 0) {
                val a = points[i - 1]
                val b = points[i]
                // Разрыв сигнала не засчитываем как пройденный путь по прямой.
                if (b.timeMs - a.timeMs <= TrackAnalysis.GAP_MS) {
                    meters += TrackAnalysis.distanceM(a.latitude, a.longitude, b.latitude, b.longitude)
                }
            }
        }
        val duration = points.last().timeMs - points.first().timeMs
        val km = meters / 1000.0
        val avg = if (duration > 0) km / (duration / 3_600_000.0) else 0.0
        return Stats(km.coerceIn(0.0, 20000.0), maxSpeed.coerceIn(0.0, 500.0), avg.coerceIn(0.0, 500.0), duration)
    }

    /** Заголовок карточки: «№ 2345 · Откуда — Куда» (плечо, иначе первая/последняя станция), не длиннее 200 символов. */
    fun title(train: Train): String {
        val phase = train.servicePhase
        val route = if (phase != null && phase.departureStation.isNotBlank() && phase.arrivalStation.isNotBlank()) {
            "${phase.departureStation} — ${phase.arrivalStation}"
        } else {
            val names = train.stations.sortedBy { it.orderIndex }.mapNotNull { it.stationName?.takeIf(String::isNotBlank) }
            if (names.size >= 2) "${names.first()} — ${names.last()}" else names.firstOrNull().orEmpty()
        }
        val number = train.number?.takeIf { it.isNotBlank() }?.let { "№ $it" }
        return listOfNotNull(number, route.takeIf { it.isNotEmpty() }).joinToString(" · ").ifEmpty { "Поезд" }.take(200)
    }
}
