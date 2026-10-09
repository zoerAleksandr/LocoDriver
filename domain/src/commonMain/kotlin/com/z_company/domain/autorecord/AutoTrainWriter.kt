package com.z_company.domain.autorecord

import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.util.ServicePhaseStationPicker
import com.z_company.domain.util.ServicePhaseStationsMerge

/** Станция справочника с координатами для привязки остановки к названию. */
object AutoStationLocator {
    /** Радиус «здесь станция» (SCREEN_SPECS §6.2 п.6а, §6.9): 1 км, общий для АВТО, карты и просмотра трека. */
    const val RADIUS_M = StopDetectionConfig.STATION_RADIUS_M

    /**
     * Название ближайшей станции справочника в радиусе [radiusM] или null (вне станций —
     * название остаётся пустым). [points] и [names] — как в `StationDirectory.points/names`.
     */
    fun nearestName(
        points: Map<String, Pair<Double, Double>>,
        names: Map<String, String>,
        latitude: Double,
        longitude: Double,
        radiusM: Double = RADIUS_M,
    ): String? =
        points.map { (key, p) -> key to ServicePhaseStationPicker.distanceM(latitude, longitude, p.first, p.second) }
            .filter { it.second <= radiusM }
            .minByOrNull { it.second }
            ?.let { names[it.first] }
}

/**
 * Название станции остановки для поезда [train]: единая функция [ServicePhaseStationPicker.pickForStop] с учётом плеча и
 * порядка станций поезда. Одинакова для АВТО, карты и просмотра трека.
 */
fun stationNameForStop(
    train: Train?,
    latitude: Double,
    longitude: Double,
    points: Map<String, Pair<Double, Double>>,
    names: Map<String, String>,
): String? = ServicePhaseStationPicker.pickForStop(
    train?.servicePhase,
    ServicePhaseStationPicker.writtenNames(train?.stations?.map { it.stationName }.orEmpty(), train?.servicePhase != null),
    latitude, longitude, points, names,
)

/**
 * Запись события АВТО в поезд по правилам кнопки «GO» (прибытие → отправление → следующая
 * станция). Чистая функция: возвращает новый поезд или null, если записывать нечего.
 *
 * Событие пишется, только если его тип совпадает с тем, что «GO» поставил бы следующим, и время
 * не раньше последнего заполненного — так ручные правки не перезаписываются и порядок не ломается.
 */
object AutoTrainWriter {

    fun apply(train: Train, event: StopEvent, stationName: String?): Train? {
        val time = event.minuteTimeMs
        val name = stationName?.takeIf { it.isNotBlank() }
        val stations = train.stations.toMutableList()
        val hasServicePhase = train.servicePhase != null

        if (stations.isEmpty()) {
            if (event.type != StopEventType.DEPARTURE) return null
            stations.add(Station(trainId = train.trainId, stationName = name, timeDeparture = time))
            return train.copy(stations = stations)
        }

        // Как в onGoClicked: при выбранном плече последняя станция (прибытие плеча) не считается.
        val endIdx = if (hasServicePhase && stations.size >= 2) stations.lastIndex - 1 else stations.lastIndex
        var lastIdx = -1
        var lastIsDeparture = false
        for (i in endIdx downTo 0) {
            val s = stations[i]
            if (s.timeDeparture != null) { lastIdx = i; lastIsDeparture = true; break }
            if (s.timeArrival != null) { lastIdx = i; lastIsDeparture = false; break }
        }

        if (lastIdx == -1) {
            if (event.type != StopEventType.DEPARTURE) return null
            stations[0] = stations[0].withDeparture(time, name)
            return train.copy(stations = stations)
        }

        val lastTime = if (lastIsDeparture) stations[lastIdx].timeDeparture!! else stations[lastIdx].timeArrival!!
        if (time < lastTime) return null

        if (!lastIsDeparture) {
            // Последнее — прибытие → ждём отправление с той же станции.
            if (event.type != StopEventType.DEPARTURE) return null
            stations[lastIdx] = stations[lastIdx].withDeparture(time, name)
        } else {
            // Последнее — отправление → ждём прибытие на следующую станцию.
            if (event.type != StopEventType.ARRIVAL) return null
            val nextIdx = lastIdx + 1
            val isPhaseArrival = hasServicePhase && nextIdx == stations.lastIndex
            // Прибытие на конечную станцию плеча (или на станцию, уже стоящую следующей и названную так же) —
            // в неё же, а не новой промежуточной (дубль «Веймарн»).
            val endMatches = isPhaseArrival && name != null && stations[nextIdx].timeArrival == null &&
                sameStation(stations[nextIdx].stationName?.takeIf { it.isNotBlank() } ?: train.servicePhase?.arrivalStation, name)
            if (endMatches) {
                stations[nextIdx] = stations[nextIdx].withArrival(time, name)
            } else if (nextIdx <= stations.lastIndex && !isPhaseArrival) {
                stations[nextIdx] = stations[nextIdx].withArrival(time, name)
            } else {
                val fresh = Station(trainId = train.trainId, stationName = name, timeArrival = time)
                if (hasServicePhase && stations.size >= 2) stations.add(stations.lastIndex, fresh)
                else stations.add(fresh)
            }
        }
        return train.copy(stations = stations)
    }

    private fun sameStation(a: String?, b: String?): Boolean =
        !a.isNullOrBlank() && !b.isNullOrBlank() &&
            ServicePhaseStationsMerge.normalizeName(a) == ServicePhaseStationsMerge.normalizeName(b)

    // Название подставляется только в пустое поле — введённое пользователем не трогаем.
    private fun Station.withDeparture(time: Long, name: String?) = copy(
        timeDeparture = time,
        stationName = stationName?.takeIf { it.isNotBlank() } ?: name,
    )

    private fun Station.withArrival(time: Long, name: String?) = copy(
        timeArrival = time,
        stationName = stationName?.takeIf { it.isNotBlank() } ?: name,
    )
}
