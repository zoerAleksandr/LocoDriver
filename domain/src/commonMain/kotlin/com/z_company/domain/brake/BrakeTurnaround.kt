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
    /** Время проведения опробования (графа 7 оборота ВУ-45: подпись и время), текст «ЧЧ:ММ». */
    val readinessTime: String = "",
    /** Подпись производившего опробование - табельный номер (графа 7 оборота ВУ-45). */
    val inspectorTab: String = "",
    /** true - запись создана автоматически по стоянке. */
    val auto: Boolean = false,
    /** Вид опробования (графа 2 оборота ВУ-45), по умолчанию «сокращённое». */
    val testKind: String = DEFAULT_TEST_KIND,
    /** Графы 3-6 оборота: заполняются только при изменении состава и веса (прицепка/отцепка вагонов), иначе прочерк: вес поезда, всего осей, нажатие требуемое и фактическое, тс. */
    val newWeight: String = "",
    val newAxles: String = "",
    val newRequired: String = "",
    val newActual: String = "",
) {
    /** Состав и вес поезда изменились: заполнены графы 3-6 оборота. */
    val compositionChanged: Boolean get() = newWeight.isNotBlank() || newAxles.isNotBlank() || newRequired.isNotBlank() || newActual.isNotBlank()

    companion object {
        const val DEFAULT_TEST_KIND = "сокращённое"
        val TEST_KINDS = listOf("сокращённое", "технологическое", "полное")
    }
}

/** Куда записать замер «В пути» (плотность) на обороте справки. */
sealed interface TurnaroundPick {
    /** Записать в существующую запись [id] (без плотности). */
    data class Existing(val id: String) : TurnaroundPick

    /** Подходящей записи нет: создать новую (класс «более 5 мин», станция пустая, время замера = сейчас). */
    data object CreateNew : TurnaroundPick

    /** Оборот не ведётся (пассажирские категории): замер никуда не пишется. */
    data object NotApplicable : TurnaroundPick
}

object TurnaroundTarget {
    /**
     * Выбор записи оборота для плотности «В пути»:
     * 1) оборот не ведётся ([applies] = false) -> [TurnaroundPick.NotApplicable];
     * 2) явно выбранная запись [targetId], если она есть и в ней ещё нет плотности;
     * 3) иначе запись БЕЗ плотности, ближайшая по времени к [nowMs] (стоянка, на которой поезд стоит сейчас, или последняя прошедшая;
     *    записи без времён - после записей со временем, из них последняя добавленная);
     * 4) иначе создать новую запись. Существующая плотность молча не затирается.
     */
    fun pick(entries: List<TurnaroundEntry>, targetId: String?, nowMs: Long, applies: Boolean): TurnaroundPick {
        if (!applies) return TurnaroundPick.NotApplicable
        if (targetId != null) {
            val t = entries.firstOrNull { it.id == targetId }
            if (t != null && t.densitySec == null) return TurnaroundPick.Existing(t.id)
        }
        val candidates = entries.withIndex().filter { it.value.densitySec == null }
        if (candidates.isEmpty()) return TurnaroundPick.CreateNew
        val best = candidates.minWithOrNull(
            compareBy<IndexedValue<TurnaroundEntry>> { distanceMs(it.value, nowMs) }
                .thenByDescending { it.value.arrivalMs ?: Long.MIN_VALUE }
                .thenByDescending { it.index },
        )!!
        return TurnaroundPick.Existing(best.value.id)
    }

    /** Расстояние от [nowMs] до стоянки записи: 0 внутри стоянки (идущая стоянка без отправления - с момента прибытия); без времён - «очень далеко». */
    fun distanceMs(e: TurnaroundEntry, nowMs: Long): Long {
        val arr = e.arrivalMs ?: e.departureMs ?: return Long.MAX_VALUE / 2
        val dep = e.departureMs
        return when {
            nowMs < arr -> arr - nowMs
            dep == null -> 0L
            nowMs <= dep -> 0L
            else -> nowMs - dep
        }
    }
}
