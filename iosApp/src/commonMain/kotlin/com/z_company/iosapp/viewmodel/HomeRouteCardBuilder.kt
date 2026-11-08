@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

/*
 * Сборка карточки маршрута `HomeIosRouteCard` (SCREEN_SPECS §4.5) — общий для
 * Главного экрана и Календаря, чтобы строки маршрутов выглядели одинаково.
 * Логика перенесена из HomeScreenIosViewModel.buildLastRoutes без изменений.
 */

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeInMonth
import com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains
import com.z_company.domain.entities.route.UtilsForEntities.isFuture
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHolidayTimeInRoute
import com.z_company.domain.entities.route.UtilsForEntities.isLongCompositionTrain
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.util.TimeCalculationContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object HomeRouteCardBuilder {

    /**
     * Карточка маршрута §4.5. [index] — позиция маршрута в списке месяца
     * (по убыванию времени явки), [count] — размер полного списка месяца:
     * `numberText` = «#${count - index}» (как в HomeScreenIosViewModel).
     */
    fun build(
        route: Route,
        index: Int,
        count: Int,
        us: UserSettings,
        ss: SalarySetting,
        month: MonthOfYear,
        displayTz: TimeZone,
    ): HomeIosRouteCard {
        val start = route.basicData.timeStartWork
        val end = route.basicData.timeEndWork
        val isDifferenceDate = start != null && end != null &&
            localDate(start, displayTz) != localDate(end, displayTz)
        val endText = when {
            end == null -> ""
            isDifferenceDate || start == null -> fmtDateMiniTime(end, displayTz)
            else -> fmtTime(end, displayTz)
        }
        val ctx = TimeCalculationContext.from(us)
        val offsetMs = us.timeZone
        val workInMonth = runCatching { route.getWorkTimeInMonth(month, ctx) }.getOrNull()
        val workTime = route.getWorkTime()
        val icons = buildList {
            if (runCatching { isHolidayTimeInRoute(month, us, route) }.getOrDefault(false)) add("holiday")
            if (route.getBreakDuration() > 0L) add("break")
            if (runCatching { isLongCompositionTrain(ss, route) }.getOrDefault(false)) add("length")
            if (runCatching { isHeavyTrains(ss, route) }.getOrDefault(false)) add("weight")
            if (runCatching { isExtendedServicePhaseTrains(ss, route) }.getOrDefault(false)) add("shoulder")
            if (route.basicData.isOnePersonOperation) add("solo")
            if ((route.getPassengerTime() ?: 0L) > 0L) add("passenger")
            if (workTime != null && workTime > 12 * HOUR) add("over12")
            if (route.trains.any { it.pusher != null }) add("pusher")
            if (route.trains.any { it.doubleTraction != null }) add("double")
            if (route.trains.any { it.doubledTrain != null }) add("coupled")
        }
        return HomeIosRouteCard(
            basicId = route.basicData.id,
            startText = start?.let { fmtDateMiniTime(it, displayTz) } ?: "",
            endText = endText,
            endTextWithDate = end?.let { fmtDateMiniTime(it, displayTz) } ?: "",
            durationText = fmtDuration(workInMonth, us.isDecimalTime),
            summary = routeSummary(route),
            numberText = "#${count - index}",
            // BUG-16 (паритет с Android): isFuture прибавляет к now смещение пояса
            // пользователя от Москвы (UserSettings.timeZone).
            isFuture = route.isFuture(offsetMs),
            isTransition = route.isTransition(offsetMs),
            icons = icons,
            isSynchronized = route.basicData.isSynchronized,
            isFavorite = route.basicData.isFavorite,
        )
    }

    /** Свёрнутая строка карточки: поезд с самым поздним отправлением с первой станции, иначе первая прочая работа. */
    private fun routeSummary(route: Route): String {
        val sortedTrains = route.trains
            .filter { it.stations.firstOrNull()?.timeDeparture != null }
            .sortedByDescending { it.stations.firstOrNull()?.timeDeparture } +
            route.trains.filter { it.stations.firstOrNull()?.timeDeparture == null }
        val train = sortedTrains.firstOrNull()
        if (train != null) {
            val tn = if (!train.number.isNullOrBlank()) "№${train.number} " else ""
            val first = train.stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
            val last = if (train.stations.size > 1) train.stations.last().stationName?.takeIf { it.isNotBlank() } else null
            val path = when {
                first != null && last != null -> "$first — $last"
                else -> first ?: last ?: ""
            }
            return "$tn$path".trim()
        }
        val otherWork = route.otherWorks.firstOrNull() ?: return ""
        val type = otherWork.workType?.takeIf { it.isNotBlank() }
        val station = otherWork.station?.takeIf { it.isNotBlank() }
        return when {
            type != null && station != null -> "$type — $station"
            else -> type ?: station ?: ""
        }
    }

    private fun localDate(ms: Long, tz: TimeZone) =
        Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz).date

    private fun two(n: Int): String = if (n < 10) "0$n" else n.toString()

    /** `dd.MM HH:mm`. */
    private fun fmtDateMiniTime(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)} ${two(t.hour)}:${two(t.minute)}"
    }

    /** `HH:mm`. */
    private fun fmtTime(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.hour)}:${two(t.minute)}"
    }

    /**
     * Длительность как `ConverterLongToTime` (§0.1): `ЧЧ:ММ` (часы не ограничены 24,
     * отрицательное → `00:00`, null → пробелы) или десятичные часы `Ч,ММ`.
     */
    private fun fmtDuration(ms: Long?, isDecimal: Boolean): String {
        if (ms == null) return if (isDecimal) "" else "          "
        if (isDecimal) {
            val totalMinutes = ms / 60_000L
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            // "%.2f" от minutes/60 с округлением половины вверх.
            val hundredths = ((minutes * 100 + 30) / 60).toInt()
            return "$hours,${two(hundredths)}"
        }
        if (ms < 0) return "00:00"
        val totalMinutes = ms / 60_000L
        val hours = totalMinutes / 60
        val minutes = (totalMinutes % 60).toInt()
        val hourText = if (hours < 10) "0$hours" else hours.toString()
        return "$hourText:${two(minutes)}"
    }

    private const val HOUR = 3_600_000L
}
