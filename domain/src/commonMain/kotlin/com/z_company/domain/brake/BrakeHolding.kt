package com.z_company.domain.brake

import kotlin.math.ceil

/**
 * Закрепление поезда на перегоне при вынужденной остановке (нет возможности удержать автотормозами).
 *
 * Источники: «Памятка локомотивным бригадам по порядку действий при вынужденной остановке поезда на перегоне из-за неисправности локомотива»
 * (распоряжение ОАО «РЖД» № ЦТ-162/р от 28.04.2025), «Порядок определения потребного количества ручных тормозов и тормозных башмаков для закрепления
 * поезда на перегоне»; нормы на 100 тс веса - табл. 4 прил. 2 Правил ТО тормозного оборудования (в памятке - табл. III.4 гл. 4 прил. 2 Правил 2014 г.).
 * Спека и решения владельца - docs/BRAKE_FEATURES_PLAN.md.
 *
 * Расчёт: башмаков `ceil((Qп + Qл) / 100 × kб)` (масса локомотива УЧИТЫВАЕТСЯ); при нехватке имеющихся `S` башмаков закреплённая масса
 * `S × 100 / kб`, остаток массы закрепляется ручными осями `ceil(остаток / 100 × kо)`. Уклон, которого нет в таблице, - ближайший БОЛЬШИЙ (ступень 0,002).
 */

/** Состав однородный: груженый (нагрузка на ось 10 тс и более) или порожний (менее 10 тс). */
enum class HoldingLoad(val title: String) {
    LOADED("Груженый (нагрузка на ось 10 тс и более)"),
    EMPTY("Порожний (менее 10 тс на ось)"),
}

data class HoldingInput(
    /** Масса поезда без локомотива, тс. */
    val trainWeight: Double,
    /** Масса локомотива, тс (учитывается). */
    val locoWeight: Double = 0.0,
    val load: HoldingLoad,
    /** Руководящий уклон участка (спуск), ‰: 18 - это 0,018. */
    val slopePermille: Double,
    /** Число тормозных башмаков, имеющихся на локомотиве. */
    val shoesAvailable: Int = 0,
    /** Число осей поезда (для проверки, хватит ли осей под ручные тормоза); null - не проверять. */
    val trainAxles: Int? = null,
)

data class HoldingResult(
    /** Ступень уклона из таблицы, ‰ (4 для 3,1‰ и т.д.); уклон округлён вверх до чётного. */
    val slopeStepPermille: Int,
    val slopeRoundedUp: Boolean,
    val totalMass: Double,
    val shoesPer100: Double,
    /** Ручных осей на 100 тс; null - в таблице нет (уклон круче 0,020: закрепляются только башмаками). */
    val manualAxlesPer100: Double?,
    val shoesRequired: Int,
    val shoesUsed: Int,
    /** Масса, закреплённая имеющимися башмаками, тс. */
    val coveredMass: Double,
    /** Масса, оставшаяся на ручные тормоза, тс (0, если башмаков хватает). */
    val remainingMass: Double,
    /** Потребное число ручных осей; null - ручными тормозами закрепить нельзя (нет нормы) или закреплять нечего. */
    val manualAxles: Int?,
    /** Вагонов, у которых надо затормозить ручной тормоз: ручной тормоз вагона действует на 4 оси; null - ручные не нужны/невозможны. */
    val manualWagons: Int?,
    /**
     * Ручных тормозов, если у вагонов раздельное торможение (два тормозных цилиндра): у каждого из [manualWagons] вагонов затормаживаются оба ручных тормоза.
     * Поэтому значение равно 2 × [manualWagons]; null - ручные не нужны/невозможны.
     */
    val manualBrakesSplit: Int?,
    /** Закрепление невозможно имеющимися средствами (нет нормы для ручных или осей поезда не хватает). */
    val impossible: Boolean,
    val warnings: List<String>,
)

object BrakeHolding {
    /** Ступени уклона таблицы, ‰: 0, 2, 4, ... 40. */
    val slopeSteps: List<Int> = (0..40 step 2).toList()

    /** Башмаков на 100 тс веса: груженый (числитель табл. 4 прил. 2 Правил), по ступеням [slopeSteps]. */
    private val shoesLoaded = listOf(0.2, 0.2, 0.2, 0.2, 0.2, 0.3, 0.4, 0.4, 0.5, 0.6, 0.6, 0.7, 0.8, 0.8, 0.9, 1.0, 1.0, 1.1, 1.2, 1.2, 1.3)

    /** Башмаков на 100 тс веса: порожний (знаменатель табл. 4 прил. 2). */
    private val shoesEmpty = listOf(0.4, 0.4, 0.4, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.2, 2.4, 2.6, 2.8, 3.0, 3.2, 3.4, 3.6, 3.8)

    /** Ручных тормозных осей на 100 тс веса, ступени 0-20 ‰ (круче нормы нет). */
    private val manualAxles = listOf(0.4, 0.4, 0.4, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8)

    const val AXLES_PER_MANUAL_WAGON = 4
    /** При раздельном торможении у вагона два ручных тормоза, каждый действует на 2 оси. */
    const val AXLES_PER_MANUAL_BRAKE_SPLIT = 2

    /** Порог нагрузки на ось, тс: 10 и более - груженый (памятка), менее - порожний. */
    const val LOADED_AXLE_LOAD = 10.0

    private const val EPS = 1e-9

    /** Груженый/порожний по нагрузке на ось = масса поезда без локомотива / число осей; null - вычислить нельзя. */
    fun loadByAxleLoad(trainWeight: Double?, axles: Int?): HoldingLoad? {
        if (trainWeight == null || axles == null || trainWeight <= 0.0 || axles <= 0) return null
        return if (trainWeight / axles >= LOADED_AXLE_LOAD - EPS) HoldingLoad.LOADED else HoldingLoad.EMPTY
    }

    /** Индекс ступени таблицы для уклона, ‰: ближайшая большая чётная; null - круче 40 ‰ (таблицы нет). */
    fun stepIndex(slopePermille: Double): Int? {
        if (slopePermille.isNaN() || slopePermille < 0.0 || slopePermille > 40.0 + EPS) return null
        return ceil(slopePermille / 2.0 - EPS).toInt().coerceAtLeast(0)
    }

    fun shoesPer100(stepIndex: Int, load: HoldingLoad): Double =
        if (load == HoldingLoad.LOADED) shoesLoaded[stepIndex] else shoesEmpty[stepIndex]

    fun manualAxlesPer100(stepIndex: Int): Double? = manualAxles.getOrNull(stepIndex)

    /** null - данные не заданы или уклон вне таблицы (круче 0,040). */
    fun calculate(input: HoldingInput): HoldingResult? {
        if (input.trainWeight <= 0.0) return null
        val idx = stepIndex(input.slopePermille) ?: return null
        val step = slopeSteps[idx]
        val total = input.trainWeight + input.locoWeight.coerceAtLeast(0.0)
        val kShoe = shoesPer100(idx, input.load)
        val kAxle = manualAxlesPer100(idx)
        val required = ceil(total / 100.0 * kShoe - EPS).toInt().coerceAtLeast(1)
        val used = input.shoesAvailable.coerceIn(0, required)
        val covered = if (used >= required) total else used * 100.0 / kShoe
        val remaining = (total - covered).coerceAtLeast(0.0)
        val warnings = mutableListOf<String>()

        var axles: Int? = null
        var wagons: Int? = null
        var brakesSplit: Int? = null
        var impossible = false
        if (remaining > EPS) {
            if (kAxle == null) {
                impossible = true
                warnings += "На уклоне круче 0,020 ручными тормозами поезд не закрепляется (нормы нет): нужны только башмаки - не хватает ${required - used}."
            } else {
                axles = ceil(remaining / 100.0 * kAxle - EPS).toInt()
                wagons = ceil(axles.toDouble() / AXLES_PER_MANUAL_WAGON - EPS).toInt()
                // При раздельном торможении на каждом выбранном вагоне затормаживаются оба ручных тормоза.
                // Нельзя брать ceil(axles / 2): это может дать, например, 7 тормозов на 4 вагона.
                brakesSplit = wagons * (AXLES_PER_MANUAL_WAGON / AXLES_PER_MANUAL_BRAKE_SPLIT)
                input.trainAxles?.let {
                    if (axles > it) {
                        impossible = true
                        warnings += "Ручных осей нужно $axles, а в поезде $it осей: закрепить имеющимися средствами нельзя."
                    }
                }
            }
        }
        return HoldingResult(
            slopeStepPermille = step,
            slopeRoundedUp = input.slopePermille != step.toDouble(),
            totalMass = total,
            shoesPer100 = kShoe,
            manualAxlesPer100 = kAxle,
            shoesRequired = required,
            shoesUsed = used,
            coveredMass = covered,
            remainingMass = remaining,
            manualAxles = axles,
            manualWagons = wagons,
            manualBrakesSplit = brakesSplit,
            impossible = impossible,
            warnings = warnings,
        )
    }
}
