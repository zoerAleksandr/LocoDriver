package com.z_company.domain.brake

import com.z_company.domain.entities.route.Station
import kotlinx.serialization.Serializable

/** Класс стоянки для оборота справки: более 5 минут / более 30 минут. */
enum class StopClass { OVER_5, OVER_30 }

/** Стоянка поезда, подходящая под отметку об опробовании в пути. */
data class StopCandidate(
    val stationId: String,
    val stationName: String?,
    val arrivalMs: Long,
    val departureMs: Long?,
    val dwellMs: Long,
    val stopClass: StopClass,
    /** Стоянка продолжается (есть прибытие, отправления ещё нет). */
    val ongoing: Boolean,
)

/**
 * Оборот справки ВУ-45 (опробования в пути): стоянки определяются по станциям раздела «Поезд» маршрута.
 * Времена прибытия/отправления уже заполняет режим «АВТО» (детектор остановок по GPS, SCREEN_SPECS §6.2 п.6а)
 * либо вводит машинист вручную, поэтому отдельный источник стоянок не нужен.
 *
 * Правило: стоянка более 5 мин - запись «Станция, Плотность»; более 30 мин - то же плюс «Время готовности»
 * и «Табельный номер вагонника». Для пассажирских поездов оборот не ведётся.
 */
object TurnaroundPlanner {
    const val OVER_5_MS = 5 * 60_000L
    const val OVER_30_MS = 30 * 60_000L

    fun classify(dwellMs: Long): StopClass? = when {
        dwellMs > OVER_30_MS -> StopClass.OVER_30
        dwellMs > OVER_5_MS -> StopClass.OVER_5
        else -> null
    }

    /** Оборот ведётся для грузовых поездов всех категорий (не для пассажирских). */
    fun applies(category: BrakeCategory): Boolean = category.group != TrainGroup.PASSENGER

    fun candidates(stations: List<Station>, nowMs: Long): List<StopCandidate> =
        stations.sortedBy { it.orderIndex }.mapNotNull { st ->
            if (st.isPassingStation) return@mapNotNull null
            val arr = st.timeArrival ?: return@mapNotNull null
            val dep = st.timeDeparture
            val ongoing = dep == null
            if (ongoing && st.isFinalStation) return@mapNotNull null
            val dwell = (dep ?: nowMs) - arr
            val cls = classify(dwell) ?: return@mapNotNull null
            StopCandidate(st.stationId, st.stationName, arr, dep, dwell, cls, ongoing)
        }
}

/** Отметка об опробовании в пути (оборот справки). Сериализуема для будущего хранения. */
@Serializable
data class TurnaroundEntry(
    val id: String,
    /** Станция раздела «Поезд», из стоянки которой создана запись (null - добавлена вручную). */
    val stationId: String? = null,
    val stationName: String = "",
    val stopClass: StopClass = StopClass.OVER_5,
    /** Время прибытия на стоянку, мс (если известно). */
    val arrivalMs: Long? = null,
    /** Время отправления, мс (если известно). */
    val departureMs: Long? = null,
    /** Плотность, с (замер секундомером или ввод). */
    val densitySec: Double? = null,
    /** Время готовности (только стоянка более 30 мин), текст «ЧЧ:ММ». */
    val readinessTime: String = "",
    /** Табельный номер вагонника (только стоянка более 30 мин). */
    val inspectorTab: String = "",
    /** true - запись создана автоматически по стоянке. */
    val auto: Boolean = false,
)
