package com.z_company.domain.brake

/**
 * Плотность тормозной сети (графа «Плотность тормозной сети поезда при II/IV положениях ручки крана машиниста» справки ВУ-45).
 * Источник: Инструкция ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277, п. 3.2.2, 9.2.4, 9.2.5, 9.2.6, 9.4.1, 11.2.4 и таблица 9.1.
 *
 * Положения ручки крана машиниста № 394/395: I - зарядка и отпуск; II - поездное; III - перекрыша без питания;
 * IV - перекрыша с питанием; V - служебное торможение (п. 9.2.4, 10.2.1).
 */

/** Группа локомотивов по таблице 9.1 (по объёму главных резервуаров). */
enum class DensityLocoGroup(val title: String, val series: List<String>, val minTimes: List<Int>) {
    G1(
        "ВЛ22м, ВЛ23, ТЭ10, ТГ106, ТГМ3, ТГМ5, ТЭМ1, ТЭМ2, ЧМЭ2, ЧМЭ3, ФД, ЛВ, Л, С, ТЭ, Э, Е",
        listOf("ВЛ22М", "ВЛ23", "ТЭ10", "ТГ106", "ТГМ3", "ТГМ5", "ТЭМ1", "ТЭМ2", "ЧМЭ2", "ЧМЭ3"),
        listOf(58, 40, 29, 25, 23, 20, 17, 15, 13, 11),
    ),
    G2("ВЛ60 (все индексы), ТЭ1, М62, ТЭМ7", listOf("ВЛ60", "ТЭ1", "М62", "ТЭМ7"), listOf(69, 46, 34, 29, 25, 22, 20, 18, 15, 13)),
    G3("ВЛ8, ТЭ2, ВЛ10 (с № 19), ВЛ11", listOf("ВЛ8", "ТЭ2", "ВЛ10", "ВЛ11"), listOf(80, 58, 46, 34, 31, 26, 23, 21, 17, 15)),
    G4("ВЛ80 (все индексы), ВЛ82, ВЛ10 (№ 1-18)", listOf("ВЛ80", "ВЛ82"), listOf(98, 69, 52, 46, 38, 33, 29, 26, 22, 20)),
    G5(
        "2ТЭ10, 2ТЭ116, 2ТЭ121, ТЭ3, ТГ16, ТГ20, ТГ102, ВЛ11м, ВЛ85, 2М62, 2М62У",
        listOf("2ТЭ10", "2ТЭ116", "2ТЭ121", "ТЭ3", "ТГ16", "ТГ20", "ТГ102", "ВЛ11М", "ВЛ85", "2М62", "2М62У"),
        listOf(104, 75, 58, 52, 40, 36, 32, 29, 24, 22),
    ),
    G6("ВЛ15, 2ТЭ10У", listOf("ВЛ15", "2ТЭ10У"), listOf(129, 93, 71, 64, 51, 45, 40, 36, 30, 28)),
    G7("3М62У", listOf("3М62У"), listOf(207, 138, 102, 87, 75, 66, 60, 51, 45, 33)),
}

object BrakeDensity {
    /** Верхние границы длины состава по колонкам таблицы 9.1 (в осях). */
    val axleBounds: List<Int> = listOf(100, 150, 200, 250, 300, 350, 400, 450, 480, 530)

    /** Колонка таблицы 9.1 для числа осей; null - длиннее 530 осей (в таблице нет). */
    fun axleColumn(axles: Int): Int? {
        if (axles <= 0) return null
        val i = axleBounds.indexOfFirst { axles <= it }
        return if (i < 0) null else i
    }

    private fun normalizeSeries(s: String): String = buildString {
        for (ch in s.trim().uppercase()) {
            if (ch == ' ' || ch == '-' || ch == '_') continue
            append(
                when (ch) { // латиница, похожая на кириллицу, -> кириллица
                    'A' -> 'А'; 'B' -> 'В'; 'C' -> 'С'; 'E' -> 'Е'; 'H' -> 'Н'; 'K' -> 'К'; 'M' -> 'М'
                    'O' -> 'О'; 'P' -> 'Р'; 'T' -> 'Т'; 'X' -> 'Х'; 'Y' -> 'У'
                    else -> ch
                },
            )
        }
    }

    /**
     * Группа по серии локомотива. Точное совпадение с серией из таблицы 9.1; для серий «всех индексов» (ВЛ80, ВЛ60) -
     * по префиксу. Не найдено - null (группу выбирает пользователь; примечание 1 к таблице: по объёму ГР).
     * ВЛ10 - группа 3 (с № 19); для ВЛ10 №1-18 нужна группа 4 - выбирается вручную.
     */
    fun groupBySeries(series: String?): DensityLocoGroup? {
        val n = normalizeSeries(series ?: return null)
        if (n.isEmpty()) return null
        for (g in DensityLocoGroup.values()) if (g.series.any { it == n }) return g
        if (n.startsWith("ВЛ80")) return DensityLocoGroup.G4
        if (n.startsWith("ВЛ60")) return DensityLocoGroup.G2
        return null
    }

    /** Поправка на зарядное давление ТМ грузового поезда (примечание 3 к табл. 9.1): 5,6-5,8 - минус 20 %, 5,3-5,5 - минус 10 %. */
    fun chargeFactor(chargePressure: Double?): Double = when {
        chargePressure == null -> 1.0
        chargePressure >= 5.6 -> 0.8
        chargePressure >= 5.3 -> 0.9
        else -> 1.0
    }

    /**
     * Наименьшее допустимое время снижения давления в ГР на 0,5 кгс/см2 при поездном положении (II), с.
     * [locoCount] - число локомотивов с объединёнными главными резервуарами (примечание 2: время пропорционально объёму).
     * null - длина состава вне таблицы.
     */
    fun minTimeSeconds(group: DensityLocoGroup, axles: Int, chargePressure: Double?, locoCount: Int = 1): Double? {
        val col = axleColumn(axles) ?: return null
        return group.minTimes[col] * chargeFactor(chargePressure) * locoCount.coerceAtLeast(1)
    }

    enum class Verdict { OK, FAIL, NO_NORM }

    data class Assessment(val verdict: Verdict, val text: String)

    /**
     * II положение, грузовой поезд: время (с) не меньше нормы таблицы 9.1.
     * [groupSelected] = false (группа локомотива не выбрана и не определена по серии) - оценка недоступна, время всё равно записывается.
     */
    fun assessFreightII(timeSec: Double, minTime: Double?, groupSelected: Boolean = true): Assessment =
        if (!groupSelected) Assessment(Verdict.NO_NORM, "Выберите группу локомотива (табл. 9.1)")
        else if (minTime == null) Assessment(Verdict.NO_NORM, "Длина состава вне таблицы 9.1: нужно 1-530 осей")
        else if (timeSec + 1e-9 >= minTime) Assessment(Verdict.OK, "Норма: не менее ${fmt(minTime)} с")
        else Assessment(Verdict.FAIL, "Плотность ниже нормы: время ${fmt(timeSec)} с, нужно не менее ${fmt(minTime)} с")

    /** IV положение (перекрыша с питанием, через >=2 мин после ступени): плотность не должна уменьшиться более чем на 10 % от II. */
    fun assessFreightIV(timeIVSec: Double, timeIISec: Double?): Assessment {
        if (timeIISec == null) return Assessment(Verdict.NO_NORM, "Сначала запишите замер II положения")
        val min = timeIISec * 0.9
        return if (timeIVSec + 1e-9 >= min) Assessment(Verdict.OK, "Допуск: не менее ${fmt(min)} с (90 % от II)")
        else Assessment(Verdict.FAIL, "Плотность упала более чем на 10 %: ${fmt(timeIVSec)} с, нужно не менее ${fmt(min)} с")
    }

    /** Контроль в пути (п. 9.3.1, 9.4.1): отличие от записанного в справке более чем на 20 % в любую сторону - сокращённое опробование. */
    fun assessRoute(timeSec: Double, timeIISec: Double?): Assessment {
        if (timeIISec == null) return Assessment(Verdict.NO_NORM, "Сначала запишите замер II положения")
        val lo = timeIISec * 0.8
        val hi = timeIISec * 1.2
        return if (timeSec + 1e-9 >= lo && timeSec <= hi + 1e-9)
            Assessment(Verdict.OK, "Допуск ±20 %: ${fmt(lo)}-${fmt(hi)} с")
        else Assessment(Verdict.FAIL, "Отличие более 20 % от справки (${fmt(lo)}-${fmt(hi)} с) - сокращённое опробование")
    }

    /**
     * Пассажирский поезд (п. 3.2.2, 9.2.3): после перекрытия комбинированного крана и 20 с выдержки давление в ТМ
     * падает не более чем на 0,2 кгс/см2 за 1 мин или 0,5 за 2,5 мин (тот же темп 0,2 кгс/см2 в минуту).
     * Замер короче 1 мин не принимается.
     */
    fun assessPassenger(timeSec: Double, dropKgf: Double?): Assessment {
        if (dropKgf == null) return Assessment(Verdict.NO_NORM, "Введите падение давления в ТМ, кгс/см2")
        if (timeSec < 60.0 - 1e-9) return Assessment(Verdict.NO_NORM, "Замер должен длиться не менее 1 мин")
        val allowed = 0.2 * minOf(timeSec, 150.0) / 60.0
        return if (dropKgf <= allowed + 1e-9) Assessment(Verdict.OK, "Допуск: не более ${fmt(allowed, 2)} кгс/см2 за ${fmt(timeSec)} с")
        else Assessment(Verdict.FAIL, "Падение ${fmt(dropKgf, 2)} кгс/см2 больше допуска ${fmt(allowed, 2)} за ${fmt(timeSec)} с")
    }

    /**
     * Наименьшее давление в ТМ хвостового вагона грузового поезда: показание не должно отличаться от зарядного давления в голове
     * более чем на 0,3 кгс/см2 (до 300 осей), 0,5 (более 300 до 400 осей включительно), 0,7 (более 400 осей) - п. 48 а)-в) прил. 2 Правил.
     * null - зарядное давление вне 4,8-5,8.
     */
    fun minTailPressure(chargePressure: Double?, axles: Int): Double? {
        val p = chargePressure ?: return null
        if (p < 4.8 || p > 5.8) return null
        val deviation = when {
            axles <= 300 -> 0.3
            axles <= 400 -> 0.5
            else -> 0.7
        }
        return kotlin.math.round((p - deviation) * 100) / 100
    }

    private fun fmt(v: Double, digits: Int = 2): String {
        val f = 100.0
        val r = kotlin.math.round(v * f) / f
        val s = if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
        return s.replace('.', ',')
    }
}
