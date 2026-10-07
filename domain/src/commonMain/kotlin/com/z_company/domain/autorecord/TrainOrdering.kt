package com.z_company.domain.autorecord

import com.z_company.domain.entities.route.Train

/**
 * Порядок поездов в UI маршрута (FormScreen) и выбор целевого поезда для «АВТО» —
 * одна функция на оба места, чтобы порядок не расходился (SCREEN_SPECS §6.2 п.6а).
 */
object TrainOrdering {
    /**
     * Порядок отображения: поезда со временем отправления первой станции — по возрастанию (старые сверху),
     * затем поезда без него — в порядке добавления (порядок входного списка).
     */
    fun sortForDisplay(trains: List<Train>): List<Train> =
        trains.filter { it.stations.firstOrNull()?.timeDeparture != null }
            .sortedBy { it.stations.firstOrNull()?.timeDeparture } +
            trains.filter { it.stations.firstOrNull()?.timeDeparture == null }

    /** У поезда нет ни одного времени станций (ни прибытия, ни отправления). */
    private fun hasNoStationTimes(t: Train): Boolean =
        t.stations.none { it.timeArrival != null || it.timeDeparture != null }

    /**
     * Целевой поезд для записи «АВТО» (форма «Поезд» открытая на поезде — обрабатывается вызывающим):
     * 1) последний в порядке UI среди поездов без единого времени станций (новый пустой поезд);
     * 2) иначе последний в порядке UI;
     * 3) нет поездов — null (вызывающий создаёт новый).
     */
    fun pickAutoTarget(trains: List<Train>): Train? {
        val sorted = sortForDisplay(trains)
        return sorted.lastOrNull { hasNoStationTimes(it) } ?: sorted.lastOrNull()
    }
}
