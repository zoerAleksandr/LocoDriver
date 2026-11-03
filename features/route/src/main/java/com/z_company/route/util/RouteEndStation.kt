package com.z_company.route.util

import com.z_company.domain.entities.route.Route

/**
 * Станция, на которой маршрут фактически закончился (где машинист встал на отдых).
 *
 * Берём самый поздний по времени «конец» среди: последней станции каждого поезда
 * (время прибытия, при отсутствии — отправления) и станции прибытия каждой поездки
 * пассажиром. Если после поезда машинист ехал пассажиром в дом отдыха — пунктом отдыха
 * будет станция прибытия пассажиром, а не поезда.
 *
 * Без времени: поезд считается последним среди поездов (из таких — позже по порядку
 * отправления), пассажир без времени — после всех поездов.
 */
fun routeEndStation(route: Route): String? {
    // rank: 0 — есть время (сравниваем по нему), 1 — поезд без времени, 2 — пассажир без времени.
    data class Candidate(val name: String, val time: Long?, val untimedRank: Int) {
        val rank get() = if (time != null) 0 else untimedRank
    }

    val candidates = buildList {
        // Порядок отправления: поезда без времени отправления — в конце (как в PWA).
        val trains = route.trains.sortedWith(
            compareBy(nullsLast()) { it.stations.firstOrNull()?.timeDeparture }
        )
        trains.forEach { train ->
            val last = train.stations.lastOrNull() ?: return@forEach
            val name = last.stationName?.takeIf { it.isNotBlank() } ?: return@forEach
            add(Candidate(name, last.timeArrival ?: last.timeDeparture, 1))
        }
        route.passengers.forEach { passenger ->
            val name = passenger.stationArrival?.takeIf { it.isNotBlank() } ?: return@forEach
            add(Candidate(name, passenger.timeArrival ?: passenger.timeDeparture, 2))
        }
    }
    if (candidates.isEmpty()) return null

    // При равенстве побеждает более поздний в списке.
    return candidates.reduce { best, c ->
        when {
            c.rank != best.rank -> if (c.rank > best.rank) c else best
            (c.time ?: 0L) >= (best.time ?: 0L) -> c
            else -> best
        }
    }.name
}
