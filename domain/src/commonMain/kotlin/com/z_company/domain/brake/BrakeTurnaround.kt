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
    /** Строки изменения состава (прицеплено/отцеплено вагонов с нажатием на ось и числом осей); пусто у старых записей. */
    val changes: List<WagonChange> = emptyList(),
) {
    /** Состав и вес поезда изменились: заполнены графы 3-6 оборота. */
    val compositionChanged: Boolean get() = newWeight.isNotBlank() || newAxles.isNotBlank() || newRequired.isNotBlank() || newActual.isNotBlank()

    /**
     * Вторая строка записи при изменении состава: «Новый вес 3100 т. (290 осей) · Потребное X тс · Фактическое Y тс»;
     * вес - «ХХХХ т.», число осей - в скобках «(ХХХ осей)»; null - состав не менялся.
     */
    val compositionSummary: String?
        get() {
            val weight = newWeight.trim().takeIf { it.isNotEmpty() }?.let { "Новый вес $it т." + (newAxles.trim().takeIf { a -> a.isNotEmpty() }?.let { a -> " ($a осей)" } ?: "") }
                ?: newAxles.trim().takeIf { it.isNotEmpty() }?.let { "Осей: ($it осей)" }
            val parts = buildList {
                weight?.let { add(it) }
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

/** Строка изменения состава: [attached] - прицеплено (иначе отцеплено), нажатие на ось, тс, и число осей (текстом, как вводится). */
@Serializable
data class WagonChange(
    val id: String,
    val attached: Boolean = false,
    val pressureText: String = "",
    val axlesText: String = "",
) {
    val pressure: Double? get() = pressureText.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
    val axles: Int? get() = axlesText.trim().toIntOrNull()?.takeIf { it > 0 }
    /** Строка учитывается в расчёте, только если заданы и нажатие, и число осей. */
    val isValid: Boolean get() = pressure != null && axles != null
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
    /** Значение цели «новая запись» (выбор пользователя в шторке). */
    const val NEW_ENTRY = "__new__"

    /**
     * Выбор записи оборота для плотности «В пути»:
     * 1) оборот не ведётся ([applies] = false) -> [TurnaroundPick.NotApplicable];
     * 2) явно выбранная запись [targetId], если она есть (в т.ч. с плотностью: повторный замер, замену подтверждает пользователь);
     * 3) иначе запись БЕЗ плотности, ближайшая по времени к [nowMs] (стоянка, на которой поезд стоит сейчас, или последняя прошедшая;
     *    записи без времён - после записей со временем, из них последняя добавленная);
     * 4) иначе создать новую запись. Существующая плотность молча не затирается.
     */
    fun pick(entries: List<TurnaroundEntry>, targetId: String?, nowMs: Long, applies: Boolean): TurnaroundPick {
        if (!applies) return TurnaroundPick.NotApplicable
        if (targetId == NEW_ENTRY) return TurnaroundPick.CreateNew
        if (targetId != null) {
            // явный выбор пользователя уважается и для записи с плотностью (повторный замер): замену подтверждает интерфейс
            val t = entries.firstOrNull { it.id == targetId }
            if (t != null) return TurnaroundPick.Existing(t.id)
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
 * Контроль плотности на промежуточной стоянке (п. 10.1.19 Правил ТО тормозного оборудования):
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

    /**
     * Жёсткая маска ввода «ЧЧ:ММ». Из [raw] берутся только цифры по порядку; первая цифра, делающая значение заведомо недопустимым, и всё после неё
     * отбрасываются: часы 0-2, при первой 2 вторая не больше 3, десятки минут 0-5; не более 4 цифр. Двоеточие подставляется само
     * (после второй цифры при наборе вперёд, при стирании - нет, чтобы не мешать удалению). [prev] - предыдущее значение поля.
     */
    fun mask(prev: String, raw: String): String {
        val digits = StringBuilder()
        for (c in raw) {
            if (c !in '0'..'9') continue
            val d = c - '0'
            val ok = when (digits.length) {
                0 -> d <= 2
                1 -> digits[0] != '2' || d <= 3
                2 -> d <= 5
                3 -> true
                else -> false
            }
            if (!ok) break
            digits.append(c)
        }
        val grow = raw.length > prev.length
        return when {
            digits.length < 2 -> digits.toString()
            digits.length == 2 -> digits.toString() + if (grow || raw.contains(':')) ":" else ""
            else -> digits.substring(0, 2) + ":" + digits.substring(2)
        }
    }

    /** Значение для записи: пусто или полное корректное «ЧЧ:ММ»; неполное - null (не сохраняется). */
    fun committable(masked: String): String? = if (masked.isEmpty()) "" else if (masked.length == 5) normalize(masked) else null

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

/**
 * Расчёт граф 3-6 оборота ВУ-45 при изменении состава (прицепка/отцепка вагонов) тем же калькулятором, что на лицевой стороне:
 * потребное нажатие = ceil(N * Q / 100) (п. 8 прил. М Инструкции 808-2022; в Правилах 2025 г. об округлении не сказано), где N берётся заново по категории поезда
 * (для порожних поездов категория зависит от числа осей: до 350 / 350-400 / 400-520, табл. 1 прил. 2 Правил ТО тормозного оборудования),
 * Q - новый вес, вводимый машинистом (+ учитываемый локомотив). Фактическое нажатие считается по осям: фактическое лицевой стороны
 * (+ локомотив) + сумма (прицеплено: оси * нажатие на ось) - сумма (отцеплено: оси * нажатие на ось); новое число осей = оси лицевой +/- оси строк.
 */
object TurnaroundComposition {
    private const val EPS = 1e-9

    /** [normBefore]/[normAfter] - норма, тс на 100 тс веса, до и после; [derivedAxles]/[actual] - по строкам изменений (null - строк нет). */
    data class Result(
        val category: BrakeCategory,
        val normBefore: Int,
        val normAfter: Int,
        val totalWeight: Double?,
        val required: Double?,
        val frontActual: Double?,
        val frontAxles: Int,
        /** Что есть на лицевой по нажатиям (сгруппировано, по возрастанию нажатия): отцепить можно только это. */
        val available: List<AxleRow>,
        val derivedAxles: Int?,
        val actual: Double?,
        val attachedTc: Double,
        val detachedTc: Double,
        val requiredManualAxles: Int?,
        val manualAxlesBefore: Int?,
        val warnings: List<String>,
    ) {
        val normChanged: Boolean get() = normBefore != normAfter
        fun enough(requiredEntered: Double? = null, actualEntered: Double? = null): Boolean? {
            val a = actualEntered ?: actual ?: return null
            val r = requiredEntered ?: required ?: return null
            return a + EPS >= r
        }
        val requiredText: String? get() = required?.toLong()?.toString()
        val actualText: String? get() = actual?.let { fmt1(it) }
        val axlesText: String? get() = derivedAxles?.toString()
    }

    fun fmt1(v: Double): String {
        val r = kotlin.math.round(v * 10) / 10
        return (if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()).replace('.', ',')
    }

    /** [base] - вход лицевой стороны (категория, локомотив, строки нажатий); null - расчёт невозможен. */
    fun compute(base: BrakeCertificateInput?, newWeight: Double?, newAxlesEntered: Int?, changes: List<WagonChange>): Result? {
        if (base == null) return null
        val valid = changes.filter { it.isValid }
        // Число осей лицевой: введённое «Всего осей» (enteredAxles), иначе сумма по строкам таблицы нажатий.
        val frontAxles = base.enteredAxles?.takeIf { it > 0 } ?: base.rows.sumOf { it.axles }
        val frontComposition = base.rows.sumOf { it.pressurePerAxle * it.axles }
        val frontActual = if (base.rows.isEmpty()) null else frontComposition + (base.loco?.let { it.pressurePerAxle * it.axles } ?: 0.0)
        val attachedTc = valid.filter { it.attached }.sumOf { it.pressure!! * it.axles!! }
        val detachedTc = valid.filter { !it.attached }.sumOf { it.pressure!! * it.axles!! }
        val derivedAxles = if (valid.isEmpty()) null else frontAxles + valid.sumOf { (if (it.attached) 1 else -1) * it.axles!! }
        val actual = if (valid.isEmpty() || frontActual == null) null else frontActual + attachedTc - detachedTc

        val warnings = mutableListOf<String>()
        valid.filter { !it.attached }.groupBy { it.pressure!! }.forEach { (p, list) ->
            val want = list.sumOf { it.axles!! }
            val have = base.rows.filter { kotlin.math.abs(it.pressurePerAxle - p) < EPS }.sumOf { it.axles }
            if (have == 0) warnings += "Такого нажатия (${fmtP(p)} тс/ось) нет на лицевой"
            else if (want > have) warnings += "Отцеплено $want осей по ${fmtP(p)} тс/ось, на лицевой только $have"
        }
        if (derivedAxles != null && derivedAxles < 0) warnings += "Отцеплено осей больше, чем в составе"

        val effectiveAxles = newAxlesEntered ?: derivedAxles
        val cat = BrakeNorms.categoryForAxles(base.category, effectiveAxles)
        val calc = if (newWeight != null && newWeight > 0.0)
            BrakeCertificateCalculator.calculate(base.copy(category = cat, compositionWeight = newWeight, rows = emptyList(), declaredAxles = null, actualManualAxles = null))
        else null
        val before = BrakeCertificateCalculator.calculate(base.copy(declaredAxles = null, actualManualAxles = null))
        return Result(
            category = cat, normBefore = base.category.norm, normAfter = cat.norm, totalWeight = calc?.totalWeight,
            required = calc?.requiredPressure, frontActual = frontActual, frontAxles = frontAxles, available = base.rows.groupBy { it.pressurePerAxle }.map { (p, l) -> AxleRow(p, l.sumOf { it.axles }) }.sortedBy { it.pressurePerAxle }, derivedAxles = derivedAxles, actual = actual,
            attachedTc = attachedTc, detachedTc = detachedTc,
            requiredManualAxles = calc?.requiredManualAxles, manualAxlesBefore = before.requiredManualAxles, warnings = warnings,
        )
    }

    private fun fmtP(v: Double): String = (if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()).replace('.', ',')

    /**
     * После правки веса/осей/строк: число осей, потребное и фактическое пересчитываются, если не правились вручную
     * (были пусты или равнялись прежнему расчёту [prev]) и сама правка относится не к этому полю; вручную введённое значение сохраняется.
     */
    fun autoFill(prevEntry: TurnaroundEntry, next: TurnaroundEntry, prev: Result?, new: Result?): TurnaroundEntry {
        if (new == null) return next
        fun pick(prevVal: String, nextVal: String, prevCalc: String?, newCalc: String?): String {
            val untouched = nextVal == prevVal
            val wasAuto = prevVal.isBlank() || (prevCalc != null && prevVal.trim() == prevCalc)
            return if (untouched && wasAuto) (newCalc ?: if (prevCalc != null && prevVal.trim() == prevCalc) "" else nextVal) else nextVal
        }
        return next.copy(
            newAxles = pick(prevEntry.newAxles, next.newAxles, prev?.axlesText, new.axlesText),
            newRequired = pick(prevEntry.newRequired, next.newRequired, prev?.requiredText, new.requiredText),
            newActual = pick(prevEntry.newActual, next.newActual, prev?.actualText, new.actualText),
        )
    }

    /** Значение поля введено вручную (отличается от расчёта). */
    fun isManual(entered: String, calculated: String?): Boolean = entered.isNotBlank() && calculated != null && entered.trim() != calculated
}

/**
 * Эталон плотности для записей оборота. Запись с видом «СП»/«ТП» и указанной плотностью САМА не оценивается (показывается только значение и пометка
 * «эталон»), но становится эталоном для следующих по времени записей без вида (только замер плотности), которые оцениваются от неё (допуск 20 %,
 * [TurnaroundDensityCheck]); до первого СП/ТП эталон - замер II лицевой стороны. Порядок - по времени опробования, иначе по времени прибытия на стоянку
 * (время суток), иначе по порядку в списке. Подтверждение в тексте Правил/808-2022 не найдено: правило машиниста-владельца (п. 10.1.19 Правил говорит об «указанной в справке» плотности).
 */
object TurnaroundReference {
    /** Эталон: [seconds] с, источник - «II» ([kind] = null) либо запись СП/ТП ([kind] "СП"/"ТП", [time] «ЧЧ:ММ» или пусто). */
    data class Ref(val seconds: Double, val kind: String?, val time: String) {
        val label: String get() = if (kind == null) "от II" else "от $kind" + if (time.isNotBlank()) " $time" else ""
    }

    /** [ref] - эталон для записи (null - эталона нет); [becomesReference] - запись СП/ТП с плотностью: не оценивается, становится эталоном. */
    data class Info(val ref: Ref?, val becomesReference: Boolean)

    private fun minutes(hhmm: String): Int? {
        val n = TimeOfDayInput.normalize(hhmm) ?: return null
        return n.substring(0, 2).toInt() * 60 + n.substring(3).toInt()
    }

    /** [minutesOfDay] - минуты суток для момента (мс) в часовом поясе пользователя. */
    fun resolve(entries: List<TurnaroundEntry>, iiSec: Double?, minutesOfDay: (Long) -> Int): Map<String, Info> {
        // ключ порядка: время опробования, иначе время прибытия; без времени - как у предыдущей записи списка
        var carry = 0
        val keyed = entries.mapIndexed { idx, e ->
            val k = minutes(e.readinessTime) ?: e.arrivalMs?.let(minutesOfDay) ?: carry
            carry = k
            Triple(k, idx, e)
        }.sortedWith(compareBy({ it.first }, { it.second }))
        val out = LinkedHashMap<String, Info>()
        var current: Ref? = iiSec?.takeIf { it > 0.0 }?.let { Ref(it, null, "") }
        for ((_, _, e) in keyed) {
            val sp = (e.testKind == TurnaroundEntry.KIND_SHORT || e.testKind == TurnaroundEntry.KIND_FULL) && e.densitySec != null
            if (sp) {
                out[e.id] = Info(null, true)
                current = Ref(e.densitySec!!, e.testKindShort, TimeOfDayInput.display(e.readinessTime).takeIf { minutes(it) != null } ?: "")
            } else {
                out[e.id] = Info(current, false)
            }
        }
        return out
    }
}
