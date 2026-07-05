package com.z_company.domain.entities.route

import com.z_company.domain.util.generateId
import kotlinx.serialization.Serializable

@Serializable
data class Passenger(
    var passengerId: String = generateId(),
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var trainNumber: String? = null,
    var stationDeparture: String? = null,
    var stationArrival: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null,
    var notes: String? = null,
    /**
     * «Явка по прибытию пассажиром»: если true — отсчёт рабочего времени маршрута
     * начинается не с явки в начальном пункте, а с прибытия пассажиром на станцию
     * [stationArrival] в момент [timeArrival]. При включении клиент выставляет
     * [BasicData.timeStartWork] = [timeArrival] (флаг взаимоисключающий среди
     * пассажиров маршрута). Default false — обратная совместимость со старыми
     * клиентами/сервером (контракт full-replace, ignoreUnknownKeys).
     */
    var isWorkStartByArrival: Boolean = false
)
