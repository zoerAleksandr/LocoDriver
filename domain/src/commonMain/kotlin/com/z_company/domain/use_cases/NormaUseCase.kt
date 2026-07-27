package com.z_company.domain.use_cases

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Единая точка расчёта нормы часов.
 *
 * Учитывает:
 * - Производственный календарь с региональными праздниками
 *   (теги дней из MonthOfYear в локальной БД, уже обновлённые через applyRegionalHolidays)
 * - Дни отвлечений из новой таблицы ReleaseDay
 * - Устаревшее поле Day.isReleaseDay (для обратной совместимости)
 *
 * Использовать вместо прямых вызовов
 * `someMonthOfYear.getPersonalNormaHours()` во всех ViewModels.
 */
class NormaUseCase(
    private val calendarUseCase: CalendarUseCase,
    private val releaseDayUseCase: ReleaseDayUseCase,
) {

    /**
     * Реактивный поток нормы часов для месяца.
     * Обновляется при любом изменении MonthOfYear или ReleaseDay в БД.
     *
     * @param year  год (например, 2026)
     * @param month месяц 0-based (0=январь, как в MonthOfYear)
     */
    fun normaHoursFlow(year: Int, month: Int): Flow<Int> =
        combine(
            calendarUseCase.loadFlowMonthOfYearListState(),
            releaseDayUseCase.getByYearMonthFlow(year, month)
        ) { months, releaseDays ->
            calculate(months, releaseDays, year, month)
        }

    /**
     * Норма часов на дату — сумма нормы по дням месяца до [upToDayInclusive]
     * включительно. Для текущего месяца передаётся сегодняшнее число (норма
     * «на текущую дату»); для завершённого месяца — полная норма (передать
     * последний день/[Int.MAX_VALUE]). Учитывает отвлечения, как и [normaHoursFlow].
     *
     * @param month месяц 0-based
     * @param upToDayInclusive последний учитываемый день месяца включительно
     */
    fun normaHoursToDateFlow(year: Int, month: Int, upToDayInclusive: Int): Flow<Int> =
        combine(
            calendarUseCase.loadFlowMonthOfYearListState(),
            releaseDayUseCase.getByYearMonthFlow(year, month)
        ) { months, releaseDays ->
            calculate(months, releaseDays, year, month, upToDayInclusive)
        }

    /**
     * Разовый синхронный расчёт нормы — для случаев где Flow не нужен.
     *
     * @param year  год
     * @param month месяц 0-based
     */
    fun getNormaHours(year: Int, month: Int): Int {
        val allMonths = calendarUseCase.loadMonthOfYearList()
        val releaseDays = releaseDayUseCase.getAll()
            .filter { it.year == year && it.month == month }
        return calculate(allMonths, releaseDays, year, month)
    }

    // ─── Внутренний расчёт ───────────────────────────────────────────────────

    private fun calculate(
        months: List<MonthOfYear>,
        releaseDays: List<ReleaseDay>,
        year: Int,
        month: Int,
        upToDayInclusive: Int = Int.MAX_VALUE,
    ): Int {
        // Среди дублей MonthOfYear берём с наибольшим числом дней —
        // та же логика дедупликации, что в SettingsViewModel.
        val monthOfYear = months
            .filter { it.year == year && it.month == month }
            .maxByOrNull { it.days.size }
            ?: return 0

        // Дни отвлечений из новой таблицы ReleaseDay (добавленные после миграции).
        // «Выходной» (DayOff) — перенос выходного дня, а не отвлечение: он НЕ
        // уменьшает норму месяца (работник отрабатывает другой день). Поэтому
        // DayOff-дни исключаем из набора «пропускаемых» — они считаются в норму
        // как обычные дни. Единственный эффект DayOff — ×2 при работе.
        val releaseDayNumbers = releaseDays
            .filter { it.releaseType != ReleaseType.DayOff }
            .map { it.dayOfMonth }
            .toSet()

        var norma = 0
        monthOfYear.days.forEach { day ->
            // Считаем норму только по дням до указанной даты включительно
            if (day.dayOfMonth > upToDayInclusive) return@forEach
            // Пропускаем норму-уменьшающие отвлечения: устаревший флаг isReleaseDay
            // (кроме DayOff) ИЛИ день в таблице ReleaseDay (уже без DayOff).
            val isNormReducingRelease =
                (day.isReleaseDay && day.releaseType != ReleaseType.DayOff) ||
                    day.dayOfMonth in releaseDayNumbers
            if (!isNormReducingRelease) {
                norma += when (day.tag) {
                    TagForDay.WORKING_DAY   -> 8
                    TagForDay.SHORTENED_DAY -> 7
                    TagForDay.NON_WORKING_DAY -> 0
                    TagForDay.HOLIDAY       -> 0
                }
            }
        }
        return norma
    }
}
