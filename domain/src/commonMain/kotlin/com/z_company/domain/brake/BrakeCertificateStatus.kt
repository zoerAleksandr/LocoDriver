package com.z_company.domain.brake

/** Уровень вердикта по нажатию: цвет блоков на экране (нейтральный/зелёный, жёлтый, красный). */
enum class StatusLevel {
    /** Данных ещё нет. */
    NONE,

    /** Нажатие не ниже нормы табл. 1 прил. 2. */
    OK,

    /** Нажатия не хватает, но скорость табл. 1 прил. 2 не снижается (например, п. 3.4 прил. 2: композиционные колодки). */
    WARNING,

    /** Скорость снижена (п. 35), нажатие ниже минимума или скорость по таблицам не определена. */
    DANGER,
}

data class BrakeStatus(val level: StatusLevel, val headline: String)

object BrakeStatusResolver {
    /** Расчёт скорости выполнен: введены вес, оси и нажатие на ось (фактическое нажатие и вес больше нуля). */
    fun isCalculated(result: BrakeCertificateResult?): Boolean =
        result != null && result.actualPressure > 0.0 && result.totalWeight > 0.0


    private fun fmt(v: Double): String {
        val r = kotlin.math.round(v * 100) / 100
        val s = if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
        return s.replace('.', ',')
    }

    /** Скорость для показа в плашке: есть только когда расчёт выполнен и скорость определена (OK, не снижена или снижена до N); иначе null - плашка показывает только текст. */
    fun speedKmh(result: BrakeCertificateResult?): Int? =
        if (isCalculated(result)) (result!!.speed as? SpeedLimit.Allowed)?.kmh else null

    /** Вердикт и короткая формулировка для закреплённого блока. [hasData] - введены вес и нажатие. */
    fun resolve(result: BrakeCertificateResult?, hasData: Boolean): BrakeStatus {
        if (result == null || !hasData) return BrakeStatus(StatusLevel.NONE, "Введите вес и оси")
        if (result.enough) return BrakeStatus(StatusLevel.OK, "Нажатие достаточно")
        val lack = "Не хватает ${fmt(result.shortagePer100)} тс на 100 тс веса"
        return when (val sp = result.speed) {
            is SpeedLimit.Allowed ->
                if (sp.reducedFrom == null) {
                    BrakeStatus(StatusLevel.WARNING, "$lack; скорость не снижена (${InstructionRef.p("3.4")})")
                } else {
                    BrakeStatus(StatusLevel.DANGER, "$lack; скорость снижена (${InstructionRef.p(if (sp.steps != null) "4.4" else "3.4")})")
                }
            is SpeedLimit.NotDetermined ->
                if (sp.kind == SpeedLimit.Kind.BELOW_MIN) {
                    BrakeStatus(StatusLevel.DANGER, "Нажатие ниже минимального (${InstructionRef.table("2")}). Скорость по таблицам не определена")
                } else {
                    BrakeStatus(StatusLevel.DANGER, "$lack; скорость по таблицам не определена")
                }
        }
    }
}
