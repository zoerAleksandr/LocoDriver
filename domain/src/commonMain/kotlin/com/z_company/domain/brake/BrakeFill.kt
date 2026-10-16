package com.z_company.domain.brake

/** Результат подстановки данных локомотива из маршрута. */
data class LocoFill(
    val series: String,
    val number: String,
    /** Группа, выбранная вручную (null - автоподбор по серии и номеру). */
    val manualGroup: DensityLocoGroup?,
    /** Группа вручную сохранена (не переопределена данными маршрута). */
    val manualGroupKept: Boolean,
    /** Итоговая группа: ручная либо автоподбор по серии и номеру (null - «Не выбрана»). */
    val effectiveGroup: DensityLocoGroup?,
)

/**
 * Правила «Заполнить из маршрута» для локомотива: серия и номер всегда берутся из АКТУАЛЬНЫХ данных маршрута (последний локомотив), а не из состояния,
 * группа по объёму ГР определяется заново по новой серии и номеру. Выбранную вручную группу не затираем, пока пользователь не подтвердил замену ([force]).
 */
object BrakeFillResolver {
    fun resolveLoco(
        routeSeries: String?,
        routeNumber: String?,
        curSeries: String,
        curNumber: String,
        manualGroup: DensityLocoGroup?,
        force: Boolean,
    ): LocoFill {
        val series = routeSeries?.trim()?.takeIf { it.isNotEmpty() } ?: curSeries
        val number = routeNumber?.trim()?.takeIf { it.isNotEmpty() } ?: curNumber
        val keep = manualGroup != null && !force
        val manual = if (keep) manualGroup else null
        return LocoFill(series, number, manual, keep, manual ?: BrakeDensity.groupForLoco(series, number))
    }

    /** Значение поля прицепа/состава: подставляется, если пусто, совпадает, [force], либо старое значение - прошлая автоподстановка (пользователь его не менял). */
    fun shouldReplace(old: String, new: String, lastAutoFilled: String?, force: Boolean, same: (String, String) -> Boolean = { a, b -> a.trim() == b.trim() }): Boolean =
        old.isBlank() || same(old, new) || force || (lastAutoFilled != null && old.trim() == lastAutoFilled.trim())
}
