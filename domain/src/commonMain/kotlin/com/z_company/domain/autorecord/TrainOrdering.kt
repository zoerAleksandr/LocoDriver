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

    /**
     * Итоговая цель записи «АВТО»: запомненный поезд (нажатие «АВТО» на экране «Поезд» или фиксация
     * при включении без контекста), если он есть среди поездов текущего маршрута; иначе
     * (не задан / удалён / из другого маршрута) — правило [pickAutoTarget].
     */
    fun pickTarget(trains: List<Train>, preferredId: String?): Train? =
        preferredId?.let { id -> trains.firstOrNull { it.trainId == id } } ?: pickAutoTarget(trains)

    /**
     * Цель «АВТО» строго по запомненному id среди поездов ВСЕХ переданных маршрутов (вызывающий
     * отфильтровывает удалённые): цель может быть в любом, в том числе ещё не начавшемся маршруте.
     * Найден — это и есть цель (с `basicId` его маршрута). Не найден / не задан — null: вызывающий
     * применяет [pickTarget] на текущем маршруте.
     */
    fun findTargetTrain(routesTrains: List<List<Train>>, preferredId: String?): Train? =
        preferredId?.let { id -> routesTrains.firstNotNullOfOrNull { l -> l.firstOrNull { it.trainId == id } } }

    /**
     * Цель плитки «Карта» на Главном (SCREEN_SPECS §4): поезд текущего маршрута по правилу «АВТО»
     * ([pickAutoTarget]); null - нет текущего маршрута (`routeTrains == null`) или в нём нет поездов:
     * тогда открывается только карта с текущим положением, без создания данных.
     */
    fun pickHomeMapTarget(routeTrains: List<Train>?): Train? = routeTrains?.let { pickAutoTarget(it) }

    /** Куда писать трек по кнопке записи на карте (SCREEN_SPECS §6.9). */
    sealed interface RecordTarget {
        /** Писать в существующий поезд. */
        data class Existing(val trainId: String) : RecordTarget

        /** Создать пустой поезд в маршруте [routeId]. */
        data class NewTrainInRoute(val routeId: String) : RecordTarget

        /** Текущего маршрута нет: писать локальный трек без маршрута и без поезда (`NO_TRAIN_ID`). */
        object NoTrain : RecordTarget
    }

    /**
     * Цель кнопки записи на карте: карта открыта из «Поезда» ([openedTrainId]) - этот поезд; иначе (с Главного) -
     * поезд текущего маршрута по правилу «АВТО» ([pickHomeMapTarget]); в текущем маршруте поездов нет - новый поезд
     * в нём; текущего маршрута нет ([currentRouteId] == null) - трек без поезда ([RecordTarget.NoTrain]); маршрут не создаётся.
     */
    fun pickRecordTarget(openedTrainId: String?, currentRouteId: String?, currentRouteTrains: List<Train>?): RecordTarget {
        if (openedTrainId != null) return RecordTarget.Existing(openedTrainId)
        pickHomeMapTarget(currentRouteTrains)?.let { return RecordTarget.Existing(it.trainId) }
        return if (currentRouteId != null) RecordTarget.NewTrainInRoute(currentRouteId) else RecordTarget.NoTrain
    }
}
