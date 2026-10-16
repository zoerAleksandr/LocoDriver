package com.z_company.domain.brake

/**
 * Нормативные таблицы для расчёта справки ВУ-45.
 * Источник: Инструкция ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277 (МПС, 16.05.1994), приложение 2 «Нормы обеспечения поездов
 * тормозами и допускаемые скорости движения» (табл. 1, 2, 3, 6; п. 3.3, 4.4, 6.4, 7). Редакция 1994 г. —
 * актуальность перед использованием в работе нужно сверять с действующими документами ОАО «РЖД».
 */

/** Крутизна руководящего спуска. */
enum class GradeClass(val title: String) {
    UP_TO_006("до 0,006"),
    OVER_006_TO_010("круче 0,006 до 0,010"),
    OVER_010_TO_015("круче 0,010 до 0,015"),
    /** Круче 0,015: норм табл. 1/2 нет, скорость устанавливает начальник дороги. */
    OVER_015("круче 0,015"),
}

/**
 * Ступени руководящего уклона по табл. 6 Норм (оси ручных тормозов на 100 тс). Для диапазона берётся верхняя граница ступени
 * (безопасная сторона). Класс [grade] определяет скорости табл. 1/2; ступени круче 0,015 норм скорости не имеют.
 */
enum class SlopeStep(val slopeText: String, val grade: GradeClass, val table6: Double) {
    S006("до 0,006", GradeClass.UP_TO_006, 0.4),
    S008("0,008", GradeClass.OVER_006_TO_010, 0.6),
    S010("0,010", GradeClass.OVER_006_TO_010, 0.8),
    S012("0,012", GradeClass.OVER_010_TO_015, 1.0),
    S014("0,014", GradeClass.OVER_010_TO_015, 1.2),
    S016("0,016", GradeClass.OVER_015, 1.4),
    S018("0,018", GradeClass.OVER_015, 1.6),
    S020("0,020", GradeClass.OVER_015, 1.8),
}

/** Группа поезда (определяет правило снижения скорости по п. 4.4 и учёт локомотива). */
enum class TrainGroup {
    PASSENGER,
    FREIGHT_PASSENGER,
    REEFER_FAST, // рефрижераторный, скорости более 90 км/ч — п. 4.4 как для пассажирских
    FREIGHT,
}

/**
 * Строка таблицы 1 (единое наименьшее тормозное нажатие) вместе с соответствующей строкой таблицы 2
 * (допускаемое минимальное нажатие), если она есть.
 *
 * Скорости: [vFlat] — числитель табл. 1 (спуск до 0,010 включительно), [vSteep] — знаменатель
 * (круче 0,010 до 0,015; для п. 11.3-11.6 — до 0,012), null — прочерк в таблице.
 * [vFlatAutoblock] — значение в скобках (автоблокировка с трёхзначной сигнализацией, зелёный огонь АЛСН).
 * [minNorm]/[minSpeeds] — табл. 2: нажатие и скорости для трёх классов спуска (null — прочерк/нет строки).
 */
data class BrakeCategory(
    val id: String,
    val tableRow: String,
    val title: String,
    val group: TrainGroup,
    val norm: Int,
    val vFlat: Int,
    val vFlatAutoblock: Int?,
    val vSteep: Int?,
    val minNorm: Int?,
    val minSpeeds: List<Int?>,
    val locoCountedByDefault: Boolean,
    val manualBrakesRequired: Boolean,
    val isLoadedFreight: Boolean = false,
)

object BrakeNorms {
    val categories: List<BrakeCategory> = listOf(
        BrakeCategory("PASS_120", "1", "Пассажирский, до 120 км/ч (ЭПТ/пневматика)", TrainGroup.PASSENGER, 60, 120, null, 110, 45, listOf(105, 90, 80), true, false),
        BrakeCategory("PASS_130", "2", "Пассажирский, до 130 км/ч (композит, ЭПТ)", TrainGroup.PASSENGER, 68, 130, null, null, 60, listOf(120, 110, null), true, false),
        BrakeCategory("PASS_140", "3", "Пассажирский, до 140 км/ч (композит, ЭПТ)", TrainGroup.PASSENGER, 78, 140, null, null, 68, listOf(130, 120, null), true, false),
        BrakeCategory("PASS_160", "4", "Пассажирский, до 160 км/ч (композит, ЭПТ)", TrainGroup.PASSENGER, 80, 160, null, null, 70, listOf(150, 140, null), true, false),
        BrakeCategory("PASS_RIC_140", "5", "Пассажирский с вагонами РИЦ (тормоз КЕ), до 140 км/ч", TrainGroup.PASSENGER, 70, 140, null, null, null, listOf(null, null, null), true, false),
        BrakeCategory("PASS_RIC_160", "6", "Пассажирский с вагонами РИЦ (тормоз КЕ), до 160 км/ч", TrainGroup.PASSENGER, 80, 160, null, null, null, listOf(null, null, null), true, false),
        BrakeCategory("EMPTY_350", "7", "Порожний грузовой до 350 осей / рефрижераторный (композит)", TrainGroup.FREIGHT, 55, 100, null, 90, 50, listOf(90, 90, 80), false, true),
        BrakeCategory("REEFER", "8", "Рефрижераторный поезд (композит), до 120 км/ч", TrainGroup.REEFER_FAST, 60, 120, null, 100, 50, listOf(110, 100, 90), false, false),
        BrakeCategory("FREIGHT_LOADED", "9", "Груженый грузовой / рефрижераторный / хозяйственный", TrainGroup.FREIGHT, 33, 80, 90, 70, 28, listOf(70, 70, 60), false, true, isLoadedFreight = true),
        BrakeCategory("FREIGHT_PASS", "10", "Грузопассажирский", TrainGroup.FREIGHT_PASSENGER, 44, 90, null, 80, 38, listOf(80, 75, 65), true, true),
        BrakeCategory("EMPTY_350_400", "11.1", "Порожний, 350-400 осей включительно", TrainGroup.FREIGHT, 44, 90, null, 80, null, listOf(null, null, null), false, true),
        BrakeCategory("EMPTY_400_520", "11.2", "Порожний, более 400 до 520 осей включительно", TrainGroup.FREIGHT, 33, 80, 90, 70, null, listOf(null, null, null), false, true),
        BrakeCategory("JOINED_UNITED", "11.3", "Соединённый до 12 тыс. тс, объединённая ТМ", TrainGroup.FREIGHT, 33, 65, 75, 60, 28, listOf(55, 55, 50), false, true),
        BrakeCategory("JOINED_SEPARATE", "11.4", "Соединённый до 12 тыс. тс, необъединённая ТМ", TrainGroup.FREIGHT, 33, 60, null, 60, 28, listOf(50, 50, 50), false, true),
        BrakeCategory("HEAD_TAIL_12", "11.5", "До 12 тыс. тс, локомотивы в голове и хвосте", TrainGroup.FREIGHT, 33, 75, 85, 65, 28, listOf(65, 50, 50), false, true),
        BrakeCategory("HEAD_THIRD_16", "11.6", "До 16 тыс. тс, локомотивы в голове и последней трети", TrainGroup.FREIGHT, 33, 70, 80, 65, 28, listOf(60, 60, 55), false, true),
    )

    /** Значения «Тормозное нажатие на ось, тс» в первой графе бланка ВУ-45 (форма по Инструкции 808-2017 ПКБ ЦВ, прил. К). */
    val blankAxlePressures: List<Double> = listOf(
        1.25, 3.0, 3.5, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0, 11.0, 11.5, 12.0, 12.5, 13.0, 14.0, 15.0, 16.0, 18.0, 19.0, 20.0, 21.0,
    )

    /** Типы вагонов табл. 3 с данным нажатием на ось (подсказка к значению бланка). */
    fun presetsFor(pressure: Double): List<AxlePreset> = axlePresets.filter { it.pressure == pressure }

    fun category(id: String): BrakeCategory? = categories.firstOrNull { it.id == id }

    /** Табл. 6: единая норма и нормы по крутизне уклона — число ручных тормозных осей на 100 тс веса состава. */
    enum class ManualBrakeNorm(val perHundred: Double, val title: String) {
        UNIFIED_06(0.6, "0,6 - единое наименьшее (по умолчанию)"),
        SLOPE_006(0.4, "0,4 - уклон до 0,006"),
        SLOPE_008(0.6, "0,6 - уклон 0,008"),
        SLOPE_010(0.8, "0,8 - уклон 0,010"),
        SLOPE_012(1.0, "1,0 - уклон 0,012"),
        SLOPE_014(1.2, "1,2 - уклон 0,014"),
        SLOPE_016(1.4, "1,4 - уклон 0,016"),
        SLOPE_018(1.6, "1,6 - уклон 0,018"),
        SLOPE_020(1.8, "1,8 - уклон 0,020"),
    }

    /** Единое наименьшее число ручных осей на 100 тс веса (п. 7: поезда в пределах двух и более дорог). */
    const val UNIFIED_MANUAL = 0.6

    /** Число ручных осей на 100 тс веса и пояснение, откуда оно получено. [override] != null - ручное значение пользователя. */
    data class ManualInfo(val perHundred: Double, val text: String, val auto: Boolean)

    fun manualInfo(slope: SlopeStep, override: ManualBrakeNorm?): ManualInfo {
        if (override != null) return ManualInfo(override.perHundred, "вручную: ${fmtK(override.perHundred)} оси на 100 тс веса", false)
        val t6 = slope.table6
        val v = maxOf(UNIFIED_MANUAL, t6)
        val text = if (t6 < UNIFIED_MANUAL) "по табл. 6 для уклона ${slope.slopeText} - ${fmtK(t6)}, но не менее единого наименьшего ${fmtK(UNIFIED_MANUAL)} (п. 7)"
        else "по табл. 6 для уклона ${slope.slopeText} - ${fmtK(t6)}"
        return ManualInfo(v, text, true)
    }

    private fun fmtK(v: Double): String = v.toString().replace('.', ',')

    /** Табл. 3: расчётные нажатия колодок на ось вагонов (в пересчёте на чугунные), тс. */
    data class AxlePreset(val title: String, val pressure: Double)

    val axlePresets: List<AxlePreset> = listOf(
        AxlePreset("Грузовой, чугун, груженый", 7.0),
        AxlePreset("Грузовой, чугун, средний", 5.0),
        AxlePreset("Грузовой, чугун, порожний", 3.5),
        AxlePreset("Грузовой, композит, груженый", 8.5),
        AxlePreset("Грузовой, композит, средний", 7.0),
        AxlePreset("Грузовой, композит, порожний", 3.5),
        AxlePreset("4-осный изотермический/багажный цельнометалл. (одностороннее)", 6.0),
        AxlePreset("Рефрижераторный, чугун, груженый", 9.0),
        AxlePreset("Рефрижераторный, чугун, средний", 6.0),
        AxlePreset("Рефрижераторный, чугун, порожний", 3.5),
        AxlePreset("Рефрижераторный, композит, средний", 7.0),
        AxlePreset("Рефрижераторный, композит, порожний", 4.5),
        AxlePreset("Рефрижераторный (ТУ до 120 км/ч), композит, средний", 14.0),
        AxlePreset("Рефрижераторный (ТУ до 120 км/ч), композит, порожний", 8.5),
        AxlePreset("Хоппер-дозатор ЦНИИ-2/3, чугун, груженый", 3.5),
        AxlePreset("Хоппер-дозатор ЦНИИ-2/3, чугун, порожний", 1.25),
        AxlePreset("Хоппер-дозатор ЦНИИ-2/3, композит, груженый", 7.0),
        AxlePreset("Хоппер-дозатор ЦНИИ-2/3, композит, порожний", 3.5),
        AxlePreset("Хоппер ЦНИИ-ДВЗ/цементовоз до 1973, чугун, груженый", 6.0),
        AxlePreset("Хоппер ЦНИИ-ДВЗ/цементовоз до 1973, чугун, порожний", 2.5),
        AxlePreset("Хоппер ЦНИИ-ДВЗ, композит, средний", 7.0),
        AxlePreset("Хоппер ЦНИИ-ДВЗ, композит, порожний", 3.0),
        AxlePreset("Хоппер ЦНИИ-ДВЗМ, чугун, груженый", 7.0),
        AxlePreset("Хоппер ЦНИИ-ДВЗМ, чугун, порожний", 3.0),
        AxlePreset("Думпкар 3ВС50/4ВС50/5ВС60, чугун, груженый", 6.0),
        AxlePreset("Думпкар 3ВС50/4ВС50/5ВС60, чугун, средний", 4.5),
        AxlePreset("Думпкар 3ВС50/4ВС50/5ВС60, чугун, порожний", 3.0),
        AxlePreset("Думпкар 6ВС60/7ВС60/ВС66/ВС95/2ВС105, чугун, груженый", 7.0),
        AxlePreset("Думпкар 6ВС60/7ВС60/ВС66/ВС95/2ВС105, чугун, средний", 4.5),
        AxlePreset("Думпкар 6ВС60/7ВС60/ВС66/ВС95/2ВС105, чугун, порожний", 3.5),
        AxlePreset("Хоппер ДВЗМ/55-76/думпкары, композит, средний", 7.0),
        AxlePreset("Хоппер ДВЗМ/55-76/думпкары, композит, порожний", 3.5),
        AxlePreset("Пассажирский цельнометаллический, тара 53 тс и более", 10.0),
        AxlePreset("Пассажирский цельнометаллический, тара 48-53 тс", 9.0),
        AxlePreset("Пассажирский цельнометаллический, тара 42-48 тс", 8.0),
        AxlePreset("Пассажирский РИЦ (КЕ, чугун), пассажирский режим", 10.0),
        AxlePreset("Пассажирский РИЦ (КЕ, чугун), скоростной режим", 15.0),
        AxlePreset("Пассажирский ВЛ-РИЦ (ТВЗ-ЦНИИ М, КЕ, композит), пассажирский режим", 10.0),
        AxlePreset("Пассажирский ВЛ-РИЦ (ТВЗ-ЦНИИ М, КЕ, композит), скоростной режим", 13.0),
        AxlePreset("Пассажирский длиной 20,2 м и менее", 9.0),
        AxlePreset("Пассажирский прочие", 6.5),
    )
}
