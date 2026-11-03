package com.z_company.domain.brake

/** Результат подстановки данных локомотива из маршрута. */
data class LocoFill(
    val series: String,
    val number: String,
    /** Группа, выбранная вручную, после подстановки (null - сброшена или не выбиралась: действует автоподбор по серии и номеру). */
    val manualGroup: DensityLocoGroup?,
    /** Свой объём ГР после подстановки (пусто - сброшен или не задавался). */
    val customVolume: String,
    /** Итоговая группа: свой объём > ручная > автоподбор по серии и номеру (null - «Не выбрана»). */
    val effectiveGroup: DensityLocoGroup?,
    /** Ручная группа / свой объём сброшены, так как маршрут содержит серию, по которой объём ГР определяется. */
    val groupReset: Boolean,
)

/**
 * Правила «Заполнить из маршрута»: нажатие сразу заменяет значения актуальными данными маршрута (в том числе введённые вручную);
 * пустые и нулевые поля маршрута введённое не затирают. Серия и номер - из последнего локомотива маршрута. Объём ГР определяется заново по серии
 * (ВЛ10 - по номеру); ручная группа и свой объём сбрасываются ТОЛЬКО если маршрут содержит серию, по которой группа определяется,
 * иначе группа не меняется.
 */
object BrakeFillResolver {
    fun resolveLoco(
        routeSeries: String?,
        routeNumber: String?,
        curSeries: String,
        curNumber: String,
        manualGroup: DensityLocoGroup?,
        customVolume: String = "",
    ): LocoFill {
        val rs = routeSeries?.trim()?.takeIf { it.isNotEmpty() }
        val rn = routeNumber?.trim()?.takeIf { it.isNotEmpty() }
        val series = rs ?: curSeries
        val number = rn ?: curNumber
        val determined = rs != null && BrakeDensity.groupForLoco(rs, rn ?: curNumber) != null
        val manual = if (determined) null else manualGroup
        val custom = if (determined) "" else customVolume
        val effective = when {
            custom.isNotBlank() -> (BrakeDensity.groupByVolume(custom) as? BrakeDensity.VolumeGroup.Found)?.group
            else -> manual ?: BrakeDensity.groupForLoco(series, number)
        }
        return LocoFill(series, number, manual, custom, effective, groupReset = determined && (manualGroup != null || customVolume.isNotBlank()))
    }

    /** Значение поля: заменяется данными маршрута, если они есть (не пусто и не ноль); иначе остаётся прежнее. */
    fun pickRouteValue(old: String, routeValue: String?): String = routeValue?.takeIf { it.isNotBlank() } ?: old

    /** Автозаполнение при открытии экрана: только пустые поля (введённое и сохранённое не затирается). */
    fun pickOnOpen(old: String, routeValue: String?): String = if (old.isBlank()) routeValue ?: old else old
}
