package com.z_company.domain.entities.route

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.UtilsForEntities.getOverRestTime
import com.z_company.domain.util.TimeCalculationContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Переотдых одной поездки: оплачиваемое время и сумма (2/3 тарифа за час). */
data class OverRestPayment(
    val timeMillis: Long,
    val money: Double,
) {
    companion object {
        val NONE = OverRestPayment(0L, 0.0)
    }
}

/**
 * Поиск соседних маршрутов для переотдыха в пункте оборота. Переотдых считается
 * «по календарю», поэтому соседи берутся независимо от месяца, к которому
 * относится сам маршрут: предыдущий маршрут может быть в прошлом месяце, а
 * следующий — в будущем. Единая точка для FormViewModel, «Все маршруты»,
 * месячного расчёта (`SalaryCalculationHelper.adjacentRoutes`) и PWA-моста.
 */
object OverRestRoutes {

    /**
     * Ближайший по явке маршрут раньше [route] — кандидат в «предыдущий» для
     * переотдыха. Сам [route] и маршруты без явки не учитываются.
     */
    fun List<Route>.previousRouteFor(route: Route): Route? {
        val start = route.basicData.timeStartWork ?: return null
        return filter { candidate ->
            candidate.basicData.id != route.basicData.id &&
                (candidate.basicData.timeStartWork ?: return@filter false) < start
        }.maxByOrNull { it.basicData.timeStartWork!! }
    }

    /**
     * Соседи месяца [monthOfYear] для месячного расчёта переотдыха: последний
     * маршрут с явкой до начала месяца и первый маршрут с явкой после его конца.
     * Границы месяца — в `crossMonthTZ`, как у всех переходных расчётов.
     * Результат не входит в отработанное время месяца — только в поиск соседей.
     */
    fun List<Route>.adjacentRoutesOfMonth(
        monthOfYear: MonthOfYear,
        context: TimeCalculationContext,
    ): List<Route> {
        val tz = context.crossMonthTZ
        val firstDay = LocalDate(monthOfYear.year, monthOfYear.month + 1, 1)
        val monthStart = firstDay.atStartOfDayIn(tz).toEpochMilliseconds()
        val nextMonthStart = firstDay.plus(1, DateTimeUnit.MONTH)
            .atStartOfDayIn(tz).toEpochMilliseconds()
        val withStart = filter { it.basicData.timeStartWork != null }
        val lastBefore = withStart
            .filter { it.basicData.timeStartWork!! < monthStart }
            .maxByOrNull { it.basicData.timeStartWork!! }
        val firstAfter = withStart
            .filter { it.basicData.timeStartWork!! >= nextMonthStart }
            .minByOrNull { it.basicData.timeStartWork!! }
        return listOfNotNull(lastBefore, firstAfter)
    }

    /**
     * Начало предыдущего месяца относительно явки [timeStartWork] (в `crossMonthTZ`):
     * нижняя граница периода, в котором ищется предыдущий маршрут для переотдыха.
     */
    fun previousMonthStartMillis(timeStartWork: Long, context: TimeCalculationContext): Long {
        val tz = context.crossMonthTZ
        val date = Instant.fromEpochMilliseconds(timeStartWork).toLocalDateTime(tz).date
        return LocalDate(date.year, date.month, 1)
            .minus(1, DateTimeUnit.MONTH)
            .atStartOfDayIn(tz)
            .toEpochMilliseconds()
    }

    /** Сумма за переотдых: 2/3 тарифа за каждый час [overRestTime]. */
    fun overRestMoney(overRestTime: Long, tariffRate: Double): Double =
        if (overRestTime > 0L) overRestTime * (tariffRate * (2.0 / 3.0)) / 3_600_000.0 else 0.0

    /**
     * Переотдых поездки [this] после отдыха в пункте оборота [previous]
     * (формула расчёта одной поездки: полный интервал без обрезки по месяцу).
     */
    fun Route.overRestPayment(previous: Route?, minTimeRest: Long, tariffRate: Double): OverRestPayment {
        val time = previous?.getOverRestTime(this, minTimeRest) ?: 0L
        return if (time > 0L) OverRestPayment(time, overRestMoney(time, tariffRate)) else OverRestPayment.NONE
    }
}
