package com.z_company.domain.entities.route

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.util.generateId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrainDataVersion(
    val stationId: String? = null,
    val stationName: String? = null,
    val weight: String? = null,
    val axle: String? = null,
    val conditionalLength: String? = null,
    val changedAt: Long = 0L
)

/**
 * Вспомогательные данные для толкача / двойной тяги / сдвоенного поезда.
 */
@Serializable
data class TrainAssist(
    var locomotiveNumber: String? = null,   // номер локомотива
    var locomotiveSeries: String? = null,   // серия локомотива
    var driverName: String? = null,         // машинист
    var notes: String? = null,              // примечание
    var isFirst: Boolean? = null            // null = не задано, true = "Я первый", false = "Я второй"
)

/**
 * Вагонник, осматривавший и закреплявший состав (автосцепку) перед прицепкой
 * к поезду. Опционален — может отсутствовать. Редактируется в «Настройках
 * поезда» тем же паттерном, что толкач/двойная тяга (add/remove-секция).
 */
@Serializable
data class CarInspector(
    var fullName: String? = null,       // ФИО
    var tabNumber: String? = null,      // табельный номер
    var couplingTime: Long? = null      // время прицепки к составу
)

@Serializable
data class Train(
    var trainId: String = generateId(),
    var basicId: String = "",
    var number: String? = null,
    var additionalNumbers: MutableList<String> = mutableListOf(),
    var distance: String? = null,
    var weight: String? = null,
    var axle: String? = null,
    var conditionalLength: String? = null,
    var stations: MutableList<Station> = mutableListOf(),
    var servicePhase: ServicePhase? = null,
    var pusher: TrainAssist? = null,            // Толкач
    var doubleTraction: TrainAssist? = null,    // Двойная тяга
    var doubledTrain: TrainAssist? = null,      // Сдвоенный поезд
    /** История изменения состава; опциональное поле обратносуместимого API. */
    var dataVersions: List<TrainDataVersion> = emptyList(),
    /** Вагонник, осматривавший/закреплявший состав. Опционально. */
    var carInspector: CarInspector? = null
)

@Serializable
data class Station(
    var stationId: String = generateId(),
    var trainId: String = "",
    @SerialName("name")
    var stationName: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null,
    var orderIndex: Int = 0,
    @SerialName("track_number")
    var trackNumber: String? = null,
    /** Конечная станция маршрута поезда. */
    var isFinalStation: Boolean = false,
    /**
     * Проходная станция — без остановки. Когда true, у станции только одно
     * время (проследования), которое хранится в [timeArrival]; [timeDeparture]
     * не используется и должно оставаться null.
     */
    var isPassingStation: Boolean = false,
    /** Путь на перегоне ПЕРЕД этой станцией (между предыдущей станцией и этой). */
    var segmentTrackNumber: String? = null,
    /** Примечание к перегону ПЕРЕД этой станцией. */
    var segmentNotes: String? = null
)
