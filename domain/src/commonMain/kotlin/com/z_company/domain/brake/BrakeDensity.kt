package com.z_company.domain.brake

/**
 * Плотность тормозной сети (графа «Плотность тормозной сети поезда при II/IV положениях ручки крана машиниста» справки ВУ-45).
 * Источник: Правила ТО тормозного оборудования (СЖТ, 2014, ред. до 2017), прил. 2: п. 48 (полное опробование грузового поезда, плотность, давление в хвосте),
 * п. 50 (пассажирский поезд), табл. IV.1; прил. 3, п. 1 (проверка плотности перед отправлением, отличие более чем на 20 %); основной текст п. 133.
 *
 * Положения ручки крана машиниста № 394/395: I - зарядка и отпуск; II - поездное; III - перекрыша без питания;
 * IV - перекрыша с питанием; V - служебное торможение (обозначения положений - по крану машиниста; в Правилах: «положение, обеспечивающее поддержание заданного давления»).
 */

/**
 * Пример серии локомотива в группе по объёму главных резервуаров: [series] - серия (для ВЛ80, ВЛ60 - все индексы), [volume] - объём ГР по справочнику,
 * [note] - как серия попала в группу. [auto] = false - серия в примерах показывается, но группа по ней автоматически НЕ выбирается (объём зависит от номера локомотива).
 * Источник всех примеров - Приложение 1 прежней Инструкции ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277, таблица «Время наполнения главных резервуаров локомотивов и МВПС»
 * (rustrain.narod.ru/biblio/tormoza/p1.htm). В действующих Правилах объёмы по сериям не приведены (на локомотиве выписка с объёмом, прим. 4 к табл. IV.1) -
 * данные ориентировочные, проверяйте по паспорту локомотива.
 */
data class SeriesExample(val series: String, val volume: String, val note: String? = null, val auto: Boolean = true) {
    val label: String get() = "$series ($volume)" + (note?.let { ", $it" } ?: "")
}

/**
 * Группа локомотивов по таблице IV.1 Правил (строка = общий объём главных резервуаров, л). Имена G1-G7 сохраняются в данных справки.
 * Объём, которого нет в таблице, принимают по ближайшему наименьшему (прим. 3 к табл. IV.1).
 */
enum class DensityLocoGroup(val liters: Int, val examples: List<SeriesExample>, val minTimes: List<Int>) {
    G1(
        1000,
        listOf(
            SeriesExample("ВЛ23", "1000 л"), SeriesExample("ВЛ22", "1000-1040 л"),
            SeriesExample("ТЭМ1", "1000 л"), SeriesExample("ТЭМ2", "1000 л"), SeriesExample("ТЭМ2У", "1000 л"), SeriesExample("ЧМЭ3", "1000 л"),
            SeriesExample("ТЭП60", "1030 л"),
            SeriesExample("ТЭП70", "1060 л"), SeriesExample("ТЭП75", "1060 л"),
            SeriesExample("ТЭП80", "1060 л"),
            SeriesExample("ВЛ65", "1020 л"), SeriesExample("ЭП1", "1020 л"),
            SeriesExample("ЧС6", "1010 л"), SeriesExample("ЧС7", "1010 л"),
            SeriesExample("ЧС8", "1010 л"), SeriesExample("ЧС200", "1010 л"),
        ),
        listOf(58, 40, 29, 25, 23, 20, 17, 15, 13, 11),
    ),
    G2(1200, listOf(SeriesExample("ВЛ60", "1200-1290 л", "все индексы")), listOf(69, 46, 34, 29, 25, 22, 20, 18, 15, 13)),
    G3(
        1500,
        listOf(
            SeriesExample("ВЛ10У", "1500 л"), SeriesExample("ВЛ10 с № 19", "1500 л", auto = false),
            SeriesExample("ВЛ11", "1500 л"),
        ),
        listOf(80, 58, 46, 34, 31, 26, 23, 21, 17, 15),
    ),
    G4(
        1800,
        listOf(
            SeriesExample("ВЛ80", "1800 л", "все индексы"), SeriesExample("ВЛ82", "1800 л"), SeriesExample("ВЛ82М", "1800 л"),
            SeriesExample("ВЛ10 № 1-18", "1960 л", auto = false),
        ),
        listOf(98, 69, 52, 46, 38, 33, 29, 26, 22, 20),
    ),
    G5(
        2000,
        listOf(
            SeriesExample("ВЛ11М", "2000 л"), SeriesExample("2ТЭ116", "2000 л"), SeriesExample("2ТЭ116УП", "2000 л"), SeriesExample("ТГ102", "2000 л"),
            SeriesExample("ВЛ85", "2100 л"),
            SeriesExample("2ТЭ10", "2040-2160 л", "индексы Л, В, М"), SeriesExample("2ТЭ121", "2040-2160 л"),
        ),
        listOf(104, 75, 58, 52, 40, 36, 32, 29, 24, 22),
    ),
    G6(
        2500,
        listOf(SeriesExample("ВЛ15", "2500 л"), SeriesExample("2ТЭ10У", "2500 л"), SeriesExample("2ТЭ10УТ", "2500 л")),
        listOf(129, 93, 71, 64, 51, 45, 40, 36, 30, 28),
    ),
    G7(3000, emptyList(), listOf(207, 138, 102, 87, 75, 66, 60, 51, 45, 33));

    /** Название строки: «1000 л». */
    val title: String get() = "$liters л"

    /** Подпись примеров серий для пикера и шторки «?». */
    val examplesText: String get() = if (examples.isEmpty()) "примеры серий не подтверждены" else examples.joinToString(", ") { it.label }
}

object BrakeDensity {
    /** Верхние границы длины состава по колонкам таблицы IV.1 (в осях). */
    val axleBounds: List<Int> = listOf(100, 150, 200, 250, 300, 350, 400, 450, 480, 530)

    /** Колонка таблицы IV.1 для числа осей; null - длиннее 530 осей (в таблице нет). */
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
     * Группа по серии локомотива из маршрута: только серии из примеров групп (источник - Приложение 1 прежней Инструкции 277, ориентировочно).
     * Для ВЛ80 и ВЛ60 - по префиксу (все индексы). Не найдено (в т.ч. ВЛ10 - объём зависит от номера) - null: группу выбирает пользователь.
     */
    fun groupBySeries(series: String?): DensityLocoGroup? {
        val n = normalizeSeries(series ?: return null)
        if (n.isEmpty()) return null
        if (n == "ВЛ10") return null
        for (g in DensityLocoGroup.values()) {
            if (g.examples.any { it.auto && normalizeSeries(it.series) == n }) return g
        }
        if (n.startsWith("ВЛ80")) return DensityLocoGroup.G4
        if (n.startsWith("ВЛ60")) return DensityLocoGroup.G2
        return null
    }

    /** Поправка на зарядное давление ТМ грузового поезда (прим. 1 к табл. IV.1 и М.3: 0,52-0,54 МПа, т.е. 5,3-5,5 кгс/см2 - минус 10 %). Других поправок в Правилах нет. */
    fun chargeFactor(chargePressure: Double?): Double =
        if (chargePressure != null && chargePressure >= 5.3 - 1e-9 && chargePressure <= 5.5 + 1e-9) 0.9 else 1.0

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
     * II положение, грузовой поезд: время (с) не меньше нормы таблицы IV.1.
     * [groupSelected] = false (группа локомотива не выбрана и не определена по серии) - оценка недоступна, время всё равно записывается.
     */
    fun assessFreightII(timeSec: Double, minTime: Double?, groupSelected: Boolean = true): Assessment =
        if (!groupSelected) Assessment(Verdict.NO_NORM, "Выберите группу локомотива (табл. IV.1)")
        else if (minTime == null) Assessment(Verdict.NO_NORM, "Длина состава вне таблицы IV.1: нужно 1-530 осей")
        else if (timeSec + 1e-9 >= minTime) Assessment(Verdict.OK, "Норма: не менее ${fmt(minTime)} с")
        else Assessment(Verdict.FAIL, "Плотность ниже нормы: время ${fmt(timeSec)} с, нужно не менее ${fmt(minTime)} с")

    /** IV положение (перекрыша с питанием, через >=2 мин после ступени): плотность не должна уменьшиться более чем на 10 % от II. */
    fun assessFreightIV(timeIVSec: Double, timeIISec: Double?): Assessment {
        if (timeIISec == null) return Assessment(Verdict.NO_NORM, "Сначала запишите замер II положения")
        val min = timeIISec * 0.9
        return if (timeIVSec + 1e-9 >= min) Assessment(Verdict.OK, "Допуск: не менее ${fmt(min)} с (90 % от II)")
        else Assessment(Verdict.FAIL, "Плотность упала более чем на 10 %: ${fmt(timeIVSec)} с, нужно не менее ${fmt(min)} с")
    }

    /** Контроль в пути (прил. 3, п. 1; п. 133 основного текста): отличие от записанного в справке более чем на 20 % в любую сторону - сокращённое опробование. */
    fun assessRoute(timeSec: Double, timeIISec: Double?): Assessment {
        if (timeIISec == null) return Assessment(Verdict.NO_NORM, "Сначала запишите замер II положения")
        val lo = timeIISec * 0.8
        val hi = timeIISec * 1.2
        return if (timeSec + 1e-9 >= lo && timeSec <= hi + 1e-9)
            Assessment(Verdict.OK, "Допуск ±20 %: ${fmt(lo)}-${fmt(hi)} с")
        else Assessment(Verdict.FAIL, "Отличие более 20 % от справки (${fmt(lo)}-${fmt(hi)} с) - сокращённое опробование")
    }

    /**
     * Пассажирский поезд (п. 50 прил. 2): после перекрытия комбинированного крана и 20 с выдержки давление в ТМ
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
