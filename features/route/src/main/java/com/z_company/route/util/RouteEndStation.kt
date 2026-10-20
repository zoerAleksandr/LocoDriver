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
 * Если времён нет, берётся последний элемент по порядку: пассажир, иначе поезд.
 */
fun routeEndStation(route: Route): String? {
    data class Candidate(val name: String, val time: Long?)

    val candidates = buildList {
        route.trains.forEach { train ->
            val last = train.stations.lastOrNull() ?: return@forEach
            val name = last.stationName?.takeIf { it.isNotBlank() } ?: return@forEach
            add(Candidate(name, last.timeArrival ?: last.timeDeparture))
        }
        route.passengers.forEach { passenger ->
            val name = passenger.stationArrival?.takeIf { it.isNotBlank() } ?: return@forEach
            add(Candidate(name, passenger.timeArrival ?: passenger.timeDeparture))
        }
    }
    if (candidates.isEmpty()) return null

    val withTime = candidates.filter { it.time != null }
    return if (withTime.isNotEmpty()) {
        withTime.maxByOrNull { it.time!! }?.name
    } else {
        // Без времён: пассажиры идут в конце списка — после поездов.
        candidates.last().name
    }
}
