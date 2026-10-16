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
    /**
     * Вид опробования (графа 2 оборота ВУ-45): [KIND_FULL] (ТП), [KIND_SHORT] (СП) или пусто - опробования не было, только замер плотности.
     * Старые записи читаются как раньше (значение «сокращённое»/«технологическое» сохраняется; отсутствующее поле = пусто).
     */
    val testKind: String = DEFAULT_TEST_KIND,
    /** Графы 3-6 оборота: заполняются только при изменении состава и веса (прицепка/отцепка вагонов), иначе прочерк: вес поезда, всего осей, нажатие требуемое и фактическое, тс. */
    val newWeight: String = "",
    val newAxles: String = "",
    val newRequired: String = "",
    val newActual: String = "",
) {
    /** Состав и вес поезда изменились: заполнены графы 3-6 оборота. */
    val compositionChanged: Boolean get() = newWeight.isNotBlank() || newAxles.isNotBlank() || newRequired.isNotBlank() || newActual.isNotBlank()

    /** Вторая строка записи при изменении состава: «Новый вес N т · Потребное X тс · Фактическое Y тс»; null - состав не менялся. Число осей не выводится. */
    val compositionSummary: String?
        get() {
            val parts = buildList {
                if (newWeight.isNotBlank()) add("Новый вес ${newWeight.trim()} т")
                if (newRequired.isNotBlank()) add("Потребное ${newRequired.trim()} тс")
                if (newActual.isNotBlank()) add("Фактическое ${newActual.trim()} тс")
            }
            return if (parts.isEmpty()) null else parts.joinToString(" · ")
        }

    /** Короткая подпись вида для таблицы: «ТП», «СП», пусто; устаревшее «технологическое» - «ТО» не вводим, показываем как есть («техн.»). */
    val testKindShort: String
        get() = when (testKind) {
            KIND_FULL -> "ТП"
            KIND_SHORT -> "СП"
            "" -> ""
            else -> "техн."
        }

    companion object {
        const val KIND_FULL = "полное"
        const val KIND_SHORT = "сокращённое"
        /** Вид по умолчанию - пусто (опробования не было). */
        const val DEFAULT_TEST_KIND = ""
        val TEST_KINDS = listOf(KIND_FULL, KIND_SHORT)
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

/**
 * Контроль плотности на промежуточной стоянке (прил. 3, п. 1 Правил ТО тормозного оборудования):
 * изменение плотности более чем на 20 % от указанной в справке ВУ-45 (в любую сторону) - сокращённое опробование.
 * Эталон - замер II (поездного) положения на лицевой стороне справки; при его отсутствии оценки нет.
 */
object TurnaroundDensityCheck {
    enum class Verdict { OK, EXCEEDED, NO_REFERENCE }

    /** [deviationPct] - отклонение в % от эталона (минус - время падения давления меньше, плотность хуже); [lo]/[hi] - допуск, с. */
    data class Result(val verdict: Verdict, val deviationPct: Double?, val lo: Double?, val hi: Double?)

    fun check(densitySec: Double, referenceIISec: Double?): Result {
        if (referenceIISec == null || referenceIISec <= 0.0) return Result(Verdict.NO_REFERENCE, null, null, null)
        val lo = referenceIISec * 0.8
        val hi = referenceIISec * 1.2
        val ok = densitySec + 1e-9 >= lo && densitySec <= hi + 1e-9
        return Result(if (ok) Verdict.OK else Verdict.EXCEEDED, (densitySec - referenceIISec) / referenceIISec * 100.0, lo, hi)
    }
}

/** Время суток «ЧЧ:ММ» для поля времени оборота: ввод, проверка диапазона, приведение старых значений («1245» -> «12:45»). */
object TimeOfDayInput {
    /** Фильтр ввода: только цифры и двоеточие (не более одного), до 5 символов; ровно 4 цифры без двоеточия -> «ЧЧ:ММ». */
    fun filter(raw: String): String {
        var colon = false
        val kept = buildString {
            for (c in raw) {
                if (c in '0'..'9') append(c)
                else if (c == ':' && !colon && isNotEmpty()) { append(c); colon = true }
            }
        }.take(5)
        return if (!kept.contains(':') && kept.length == 4) kept.substring(0, 2) + ":" + kept.substring(2) else kept
    }

    /** Нормализованное «ЧЧ:ММ» (часы 0-23, минуты 0-59) для «9:05», «905», «1245», «12:45»; иначе null. */
    fun normalize(s: String): String? {
        val t = s.trim()
        val (h, m) = when {
            t.contains(':') -> t.split(':').let { if (it.size == 2) it[0] to it[1] else return null }
            t.length == 3 -> t.substring(0, 1) to t.substring(1)
            t.length == 4 -> t.substring(0, 2) to t.substring(2)
            else -> return null
        }
        if (h.isEmpty() || h.length > 2 || m.length != 2 || !h.all { it in '0'..'9' } || !m.all { it in '0'..'9' }) return null
        val hh = h.toInt(); val mm = m.toInt()
        if (hh > 23 || mm > 59) return null
        return hh.toString().padStart(2, '0') + ":" + m
    }

    /** Для показа: старое значение без двоеточия приводится к «ЧЧ:ММ», нераспознанное показывается как есть. */
    fun display(stored: String): String = normalize(stored) ?: stored
}
