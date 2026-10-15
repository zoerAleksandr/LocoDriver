package com.z_company.domain.brake

import kotlin.math.ceil
import kotlin.math.floor

/** Строка таблицы справки: число осей с одинаковым расчётным нажатием на ось (тс). */
data class AxleRow(val pressurePerAxle: Double, val axles: Int)

/** Локомотив, учитываемый в подсчёте (табл. 4 и 5 Норм): учётный вес, число тормозных осей, нажатие на ось. */
data class LocoBrakeInput(val weight: Double, val axles: Int, val pressurePerAxle: Double)

/** Доля вагонов с композиционными колодками при нагрузке на ось более 21 тс (п. 3.3 Норм). */
enum class CompositeShare { NONE, P50, P75, P100 }

data class BrakeCertificateInput(
    val category: BrakeCategory,
    /** Вес состава (поезда без локомотива), тс. */
    val compositionWeight: Double,
    val rows: List<AxleRow>,
    val loco: LocoBrakeInput? = null,
    val grade: GradeClass = GradeClass.UP_TO_006,
    /** Автоблокировка с трёхзначной сигнализацией, зелёный огонь АЛСН (значения в скобках табл. 1). */
    val autoblock3: Boolean = false,
    val compositeShare: CompositeShare = CompositeShare.NONE,
    /** Ручное переопределение нормы ручных осей (null - «Авто» по уклону [slope], табл. 6 с нижней границей 0,6). */
    val manualNorm: BrakeNorms.ManualBrakeNorm? = null,
    val slope: SlopeStep = SlopeStep.S006,
    /** Фактическое число ручных тормозных осей, если задано. */
    val actualManualAxles: Int? = null,
    /** Число осей состава (для сверки с суммой по строкам), если известно. */
    val declaredAxles: Int? = null,
)

sealed interface SpeedLimit {
    /** Допускаемая скорость, км/ч; [reducedFrom] — норма до снижения (п. 4.4), если снижение было. */
    data class Allowed(val kmh: Int, val note: String, val reducedFrom: Int? = null) : SpeedLimit

    /** По таблицам скорость не определяется (причина в [reason]). */
    data class NotDetermined(val reason: String) : SpeedLimit
}

data class BrakeCertificateResult(
    val totalWeight: Double,
    val requiredPressure: Double,
    val actualPressure: Double,
    val actualPer100: Double,
    val enough: Boolean,
    /** Недостаток нажатия на 100 тс веса (0, если нормы достаточно). */
    val shortagePer100: Double,
    val totalAxles: Int,
    val speed: SpeedLimit,
    /** Потребное число ручных тормозных осей (null - для категории не нормируется). */
    val requiredManualAxles: Int?,
    val manualAxlesOk: Boolean?,
    val warnings: List<String>,
)

object BrakeCertificateCalculator {
    private const val EPS = 1e-9

    fun calculate(input: BrakeCertificateInput): BrakeCertificateResult {
        val cat = input.category
        val warnings = mutableListOf<String>()
        val loco = input.loco
        val totalWeight = input.compositionWeight + (loco?.weight ?: 0.0)
        val required = cat.norm * totalWeight / 100.0
        val actual = input.rows.sumOf { it.pressurePerAxle * it.axles } +
            (loco?.let { it.pressurePerAxle * it.axles } ?: 0.0)
        val axlesFromRows = input.rows.sumOf { it.axles }
        val per100 = if (totalWeight > 0) actual * 100.0 / totalWeight else 0.0
        val enough = totalWeight > 0 && actual + EPS >= required
        val shortage = if (enough) 0.0 else (cat.norm - per100).coerceAtLeast(0.0)

        input.declaredAxles?.let {
            if (it != axlesFromRows) warnings += "Осей в таблице: $axlesFromRows, в поезде указано: $it"
        }
        if (cat.group == TrainGroup.FREIGHT && loco != null) {
            warnings += "Для грузовых поездов вес и нажатие локомотива не учитываются (Нормы, п. 6.3)"
        }

        val speed = if (totalWeight <= 0) {
            SpeedLimit.NotDetermined("Не задан вес")
        } else {
            speedLimit(cat, input, per100, enough)
        }

        val manualRequired = if (cat.manualBrakesRequired && input.compositionWeight > 0) {
            ceil(input.compositionWeight * BrakeNorms.manualInfo(input.slope, input.manualNorm).perHundred / 100.0 - EPS).toInt()
        } else null
        val manualOk = if (manualRequired != null && input.actualManualAxles != null)
            input.actualManualAxles >= manualRequired else null

        return BrakeCertificateResult(
            totalWeight = totalWeight,
            requiredPressure = required,
            actualPressure = actual,
            actualPer100 = per100,
            enough = enough,
            shortagePer100 = shortage,
            totalAxles = axlesFromRows + (loco?.axles ?: 0),
            speed = speed,
            requiredManualAxles = manualRequired,
            manualAxlesOk = manualOk,
            warnings = warnings,
        )
    }

    private fun speedLimitSteep(): SpeedLimit = SpeedLimit.NotDetermined(
        "Для спуска круче 0,015 норм табл. 1 и 2 нет - скорость устанавливает начальник дороги (п. 5.2)",
    )

    private fun speedLimit(
        cat: BrakeCategory,
        input: BrakeCertificateInput,
        per100: Double,
        enough: Boolean,
    ): SpeedLimit {
        if (input.grade == GradeClass.OVER_015) return speedLimitSteep()
        val steep = input.grade == GradeClass.OVER_010_TO_015
        val base: Int? = if (steep) cat.vSteep else cat.vFlat
        if (base == null) {
            return SpeedLimit.NotDetermined(
                "Для спуска круче 0,010 норм для этой категории нет - скорость устанавливает начальник дороги",
            )
        }
        if (enough) {
            val bracket = cat.vFlatAutoblock
            return if (!steep && input.autoblock3 && bracket != null) {
                SpeedLimit.Allowed(bracket, "Табл. 1, автоблокировка с 3-значной сигнализацией (значение в скобках)")
            } else {
                SpeedLimit.Allowed(base, "Табл. 1 (нажатие не менее нормы ${cat.norm} тс на 100 тс)")
            }
        }
        // п. 3.3: тяжёлая нагрузка на ось + композиционные колодки
        if (cat.isLoadedFreight && input.compositeShare != CompositeShare.NONE) {
            val ok = when (input.compositeShare) {
                CompositeShare.P75 -> per100 >= 31.0 - EPS
                CompositeShare.P100 -> per100 >= 30.0 - EPS
                CompositeShare.NONE, CompositeShare.P50 -> false
            }
            if (ok) {
                return SpeedLimit.Allowed(
                    minOf(80, base),
                    "П. 3.3 Норм: нагрузка на ось более 21 тс, композиционные колодки, ВР на среднем режиме",
                )
            }
        }
        val minNorm = cat.minNorm
            ?: return SpeedLimit.NotDetermined(
                "Нажатие ниже нормы ${cat.norm}; допускаемый минимум для этой категории в Нормах не задан",
            )
        if (per100 + EPS < minNorm) {
            return SpeedLimit.NotDetermined(
                "Нажатие ниже допускаемого минимума ($minNorm тс на 100 тс) - скорость по таблицам не определяется",
            )
        }
        val k = when (cat.group) {
            TrainGroup.FREIGHT -> 2
            else -> if (input.grade == GradeClass.UP_TO_006) 1 else 2
        }
        val missing = ceil(cat.norm - per100 - EPS).toInt().coerceAtLeast(1)
        val reduced = base - k * missing
        val rounded = (floor(reduced / 5.0) * 5).toInt().coerceAtLeast(0)
        return SpeedLimit.Allowed(
            rounded,
            "П. 4.4 Норм: -$k км/ч за каждую недостающую тонну ($missing т), округление вниз до кратного 5; " +
                "допустимо только в случаях п. 4.2-4.3",
            reducedFrom = base,
        )
    }
}
