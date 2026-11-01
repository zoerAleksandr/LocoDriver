package com.z_company.domain.use_cases

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.WorkScheduleProfile
import kotlinx.datetime.LocalDate
import com.z_company.domain.repositories.SharedPreferencesRepositories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.z_company.domain.util.dateOfDayOrNull

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
    private val sharedPreferences: SharedPreferencesRepositories,
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
            releaseDayUseCase.getByYearMonthFlow(year, month),
            sharedPreferences.getWorkScheduleProfileFlow(),
        ) { months, releaseDays, profile ->
            calculate(months, releaseDays, year, month, profile = profile)
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
            releaseDayUseCase.getByYearMonthFlow(year, month),
            sharedPreferences.getWorkScheduleProfileFlow(),
        ) { months, releaseDays, profile ->
            calculate(months, releaseDays, year, month, upToDayInclusive, profile)
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
        return calculate(
            allMonths,
            releaseDays,
            year,
            month,
            profile = sharedPreferences.getWorkScheduleProfile(),
        )
    }

    // ─── Внутренний расчёт ───────────────────────────────────────────────────

    internal fun calculate(
        months: List<MonthOfYear>,
        releaseDays: List<ReleaseDay>,
        year: Int,
        month: Int,
        upToDayInclusive: Int = Int.MAX_VALUE,
        profile: WorkScheduleProfile = WorkScheduleProfile.standard(),
    ): Int = buildMonthNorm(months, releaseDays, year, month, profile)
        .filter { it.day <= upToDayInclusive }
        .sumOf { it.countedHours }
}

/**
 * Норма по дням месяца: те же правила, что в [calculate] (он суммирует этот список), плюс
 * данные для календаря нормы («Своя настройка»). Пустой список — месяца нет в календаре.
 *
 * [MonthNormDay.hours] — норма дня по профилю (праздник и личный «Выходной» в режиме
 * «Своя настройка» — 0); [MonthNormDay.countedHours] — то, что идёт в норму месяца
 * (0 для дней, закрытых отпуском/больничным и т.п.).
 */
fun buildMonthNorm(
    months: List<MonthOfYear>,
    releaseDays: List<ReleaseDay>,
    year: Int,
    month: Int,
    profile: WorkScheduleProfile,
): List<MonthNormDay> {
    // Среди дублей MonthOfYear берём с наибольшим числом дней —
    // та же логика дедупликации, что в SettingsViewModel.
    val monthOfYear = months
        .filter { it.year == year && it.month == month }
        .maxByOrNull { it.days.size }
        ?: return emptyList()

    // «Выходной» (DayOff) — перенос выходного дня, а не отвлечение: он НЕ
    // уменьшает норму месяца (работник отрабатывает другой день). Поэтому
    // DayOff-дни исключаем из набора «пропускаемых» — они считаются в норму
    // как обычные дни. Единственный эффект DayOff — ×2 при работе.
    // Исключение — режим «Своя настройка»: там «Выходной» это день графика без нормы
    // (WorkScheduleProfile.effectiveHours).
    // «Технические занятия» тоже НЕ уменьшают норму: они оплачиваются
    // отдельной строкой по среднему часу за явно заданные часы
    // (см. UtilForMonthOfYear.reducesNorma — правило должно совпадать).
    val releaseDayNumbers = releaseDays
        .filter {
            it.releaseType != ReleaseType.DayOff &&
                it.releaseType != ReleaseType.BusinessTrip &&
                it.releaseType != ReleaseType.TechnicalStudy
        }
        .map { it.dayOfMonth }
        .toSet()
    val dayOffNumbers = releaseDays
        .filter { it.releaseType == ReleaseType.DayOff }
        .map { it.dayOfMonth }
        .toSet()

    return monthOfYear.days.sortedBy { it.dayOfMonth }.mapNotNull { day ->
        val date = dateOfDayOrNull(year, month, day.dayOfMonth) ?: return@mapNotNull null
        // Пропускаем норму-уменьшающие отвлечения: устаревший флаг isReleaseDay
        // (кроме DayOff и «Технических занятий») ИЛИ день в таблице ReleaseDay
        // (уже без DayOff и «Технических занятий»).
        val isNormReducingRelease =
            (day.isReleaseDay &&
                day.releaseType != ReleaseType.DayOff &&
                day.releaseType != ReleaseType.BusinessTrip &&
                day.releaseType != ReleaseType.TechnicalStudy) ||
                day.dayOfMonth in releaseDayNumbers
        val isDayOff = day.dayOfMonth in dayOffNumbers ||
            (day.isReleaseDay && day.releaseType == ReleaseType.DayOff)
        val hours = profile.effectiveHours(date, day.tag, isDayOff)
        MonthNormDay(
            day = day.dayOfMonth,
            date = date,
            tag = day.tag,
            hours = hours,
            isDayOff = isDayOff,
            isNormReducing = isNormReducingRelease,
            isCustom = day.tag != TagForDay.HOLIDAY && profile.customHoursFor(date) != null,
            countedHours = if (isNormReducingRelease) 0 else hours,
        )
    }
}

/** День месяца в расчёте нормы, см. [NormaUseCase.buildMonthNorm]. */
data class MonthNormDay(
    val day: Int,
    val date: LocalDate,
    val tag: TagForDay,
    val hours: Int,
    val isDayOff: Boolean,
    /** День закрыт отпуском/больничным и т.п. — в норму месяца не входит. */
    val isNormReducing: Boolean,
    /** Норма дня задана пользователем в календаре нормы. */
    val isCustom: Boolean,
    val countedHours: Int,
)
