@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.displayTimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Общие помощники экранов «Календарь» / «Заполнить месяц» / «Отвлечения» / «Норма по месяцам»:
 * форматирование времени в поясе отображения (порт DateAndTimeConverter / ConverterLongToTime
 * из core_android, которого нет в iOS-таргете) и гейт бесплатного лимита (порт RouteActionsHelper).
 */

internal fun iosTwo(v: Int): String = v.toString().padStart(2, '0')

/** Смещение пояса отображения в мс: `displayTimeZone()` = "GMT+N" (§0.1). */
internal fun iosDisplayOffsetMs(settings: UserSettings?): Long {
    val tz = settings?.displayTimeZone() ?: "GMT+3"
    val hours = tz.removePrefix("GMT").removePrefix("UTC").toIntOrNull() ?: 3
    return hours * 3_600_000L
}

internal fun iosDisplayTimeZone(settings: UserSettings?): TimeZone =
    runCatching { TimeZone.of(settings?.displayTimeZone() ?: "GMT+3") }.getOrElse { TimeZone.of("GMT+3") }

internal fun iosLocal(ms: Long, offset: Long) =
    Instant.fromEpochMilliseconds(ms + offset).toLocalDateTime(TimeZone.UTC)

/** `ЧЧ:ММ` (`getTimeInStringFormat`). */
internal fun iosFormatHhMm(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalMinutes = ms / 60_000
    return "${iosTwo((totalMinutes / 60).toInt())}:${iosTwo((totalMinutes % 60).toInt())}"
}

/** `H,dd` (`getTimeInStringDecimalFormat`) при isDecimalTime. */
internal fun iosFormatDecimal(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val fraction = ((totalMinutes % 60) / 60.0 * 100).let { kotlin.math.round(it).toInt() }
    return "$hours,${iosTwo(fraction)}"
}

internal fun iosFormatWorkTime(ms: Long, settings: UserSettings?): String =
    if (settings?.isDecimalTime == true) iosFormatDecimal(ms) else iosFormatHhMm(ms)

/** `HH:mm` времени суток из time-of-day миллисов (0…86_400_000). */
internal fun iosFormatTimeOfDay(millis: Long): String = iosFormatHhMm(millis)

/**
 * Локальная дата/время из epoch-мс в поясе отображения и обратно
 * (порт `DateAndTimeConverter.toEpochMillis/getDayOfMonth/getMonthIndex`).
 */
internal fun iosToEpochMillis(
    year: Int, month0: Int, day: Int, hour: Int, minute: Int, settings: UserSettings?,
): Long {
    val tz = iosDisplayTimeZone(settings)
    return LocalDate(year, month0 + 1, day)
        .atStartOfDayIn(tz)
        .toEpochMilliseconds() + hour * 3_600_000L + minute * 60_000L
}

internal fun iosDayOfMonth(millis: Long, settings: UserSettings?): Int =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(iosDisplayTimeZone(settings)).dayOfMonth

internal fun iosMonthIndex(millis: Long, settings: UserSettings?): Int =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(iosDisplayTimeZone(settings)).monthNumber - 1

internal fun iosDaysInMonth(year: Int, month0: Int): Int = when (month0) {
    1 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    3, 5, 8, 10 -> 30
    else -> 31
}

internal val IOS_MONTH_NAMES = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)

internal fun iosMonthName(index: Int): String = IOS_MONTH_NAMES.getOrElse(index) { "" }

/** Родительный падеж месяца («ноября») — для периодов «10–24 ноября». */
internal val IOS_MONTH_GENITIVE = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

internal fun iosMonthGenitive(index: Int): String = IOS_MONTH_GENITIVE.getOrElse(index) { "" }

internal fun iosPlural(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m100 in 11..14 -> many
        m10 == 1 -> one
        m10 in 2..4 -> few
        else -> many
    }
}

/** Строковый ключ типа отвлечения для Swift ("vacation" | "sick" | …). */
internal fun ReleaseType.iosKind(): String = when (this) {
    is ReleaseType.Vacation -> "vacation"
    is ReleaseType.SickLeave -> "sick"
    is ReleaseType.Courses -> "courses"
    is ReleaseType.Donor -> "donor"
    is ReleaseType.ChildCare -> "childCare"
    is ReleaseType.DayOff -> "dayOff"
    is ReleaseType.BusinessTrip -> "businessTrip"
    is ReleaseType.TechnicalStudy -> "technicalStudy"
    is ReleaseType.Other -> "other"
}

/** Короткий бейдж отвлечения для ячейки календаря (§14.2): «ОТП», «Б/Л», … */
internal fun ReleaseType.iosBadge(): String = when (this) {
    is ReleaseType.Vacation -> "ОТП"
    is ReleaseType.SickLeave -> "Б/Л"
    is ReleaseType.Courses -> "КУР"
    is ReleaseType.Donor -> "ДОН"
    is ReleaseType.ChildCare -> "УХ"
    is ReleaseType.DayOff -> "ВЫХ"
    is ReleaseType.BusinessTrip -> "КОМ"
    is ReleaseType.TechnicalStudy -> "ТЕХ"
    is ReleaseType.Other -> "ОТВ"
}

/** Норма-часы дня отвлечения (§14.1 / §16.1): см. CalendarViewModel.releaseHoursForDay. */
internal fun iosReleaseHoursForDay(
    type: ReleaseType,
    date: LocalDate,
    tag: TagForDay,
    profile: WorkScheduleProfile,
): Int =
    if (type == ReleaseType.DayOff || type == ReleaseType.TechnicalStudy) {
        0
    } else if (type == ReleaseType.ChildCare) {
        when (tag) {
            TagForDay.WORKING_DAY -> 8
            TagForDay.SHORTENED_DAY -> 7
            TagForDay.NON_WORKING_DAY -> 8
            TagForDay.HOLIDAY -> 8
        }
    } else {
        profile.effectiveHours(date, tag)
    }

/**
 * Названия федеральных праздников РФ (ст.112 ТК РФ), ключ — (месяц 0-based, число).
 * Переносы выходных названия не имеют — «Праздничный день».
 */
internal val IOS_RU_FEDERAL_HOLIDAY_NAMES: Map<Pair<Int, Int>, String> = buildMap {
    for (d in 1..8) put(0 to d, "Новогодние каникулы")
    put(0 to 7, "Рождество Христово")
    put(1 to 23, "День защитника Отечества")
    put(2 to 8, "Международный женский день")
    put(4 to 1, "Праздник Весны и Труда")
    put(4 to 9, "День Победы")
    put(5 to 12, "День России")
    put(10 to 4, "День народного единства")
}

internal fun iosHolidayName(month0: Int, day: Int, regionNames: Map<Int, String>, country: String): String? =
    regionNames[day] ?: if (country == "RU") IOS_RU_FEDERAL_HOLIDAY_NAMES[month0 to day] else null

/**
 * Гейт бесплатного лимита маршрутов (порт `RouteActionsHelper.canCreateRoutes` /
 * `freeRoutesLeft` / `hasActiveSubscription`). Лимит — 20 маршрутов, грейс-период 24 ч.
 */
internal class IosRouteGate(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
) {
    companion object {
        const val FREE_ROUTES_LIMIT = 20
        private const val GRACE_PERIOD_MS = 24L * 3_600_000L
    }

    /** "allowed" | "limit" | "error"; [freeAfter] — сколько бесплатных останется (null = подписка). */
    data class GateResult(val type: String, val requested: Int, val remaining: Int, val freeAfter: Int)

    suspend fun hasActiveSubscription(): Boolean {
        val setting = settingsUseCase.getUserSettingFlow().first()
        return setting.subscriptionPeriod > nowMs()
    }

    suspend fun freeRoutesLeft(): Int? {
        val setting = settingsUseCase.getUserSettingFlow().first()
        val time = setting.subscriptionPeriod
        val subscriptionActive = time != 0L && time + GRACE_PERIOD_MS >= nowMs()
        if (subscriptionActive) return null
        val used = withContext(Dispatchers.Default) { routeUseCase.listRouteWithDeleting().size }
        return (FREE_ROUTES_LIMIT - used).coerceAtLeast(0)
    }

    suspend fun canCreateRoutes(count: Int): GateResult = try {
        val left = freeRoutesLeft()
            ?: return GateResult("allowed", count, 0, 0)
        if (count <= left) {
            GateResult("allowed", count, 0, left - count)
        } else {
            GateResult("limit", count, left, 0)
        }
    } catch (t: Throwable) {
        GateResult("error", count, 0, 0)
    }

    private fun nowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
}

/** Формат «2 800» / «2 800,50» для «К начислению» техзанятий (§14.6). */
internal fun iosFormatMoney(value: Double): String {
    val whole = kotlin.math.floor(value).toLong()
    val cents = kotlin.math.round((value - whole) * 100).toInt()
    val grouped = whole.toString().reversed().chunked(3).joinToString(" ").reversed()
    return if (cents == 0) grouped else "$grouped,${iosTwo(cents)}"
}
