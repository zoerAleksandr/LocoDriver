@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.OverRestRoutes.overRestPayment
import com.z_company.domain.entities.route.OverRestRoutes.previousRouteFor
import com.z_company.domain.entities.route.UtilsForEntities.getPureWorkTime
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.salary.SalaryCalculationHelper
import com.z_company.domain.util.usesOnePersonPassengerRate
import com.z_company.domain.util.toMoneyString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/*
 * Модели экрана «Все маршруты» (SCREEN_SPECS §9) и общие форматтеры для
 * iOS-ViewModel маршрутов. Все строки готовые — Swift только раскладывает.
 */

/** Фильтры списка (§9.0.1). Названия — как у Android `RouteFilter` (ключи SharedPreferences). */
enum class RouteFilter {
    ALL,
    FAVORITES,
    HEAVY,
    EXTENDED_SERVICE,
    FOLLOWING_RESERVE,
    ONE_PERSON,
    OVER_12_HOURS,
    LONG_TRAINS,
    HAS_BREAK,
    PUSHER,
    DOUBLE_TRACTION,
    DOUBLED_TRAIN
}

/** Сортировка списка (§9.0.1). Названия — как у Android `SortOption`. */
enum class SortOption {
    DATE_ASC,
    DATE_DESC,
    WORKTIME_ASC,
    WORKTIME_DESC
}

/** Карточка маршрута `ItemHomeScreen` (§9.0.1). */
data class AllRouteCardUi(
    val basicId: String,
    /** «#N» — позиция снизу текущего отсортированного списка. */
    val numberText: String,
    /** «№123» или «№б/н». */
    val routeNumberText: String,
    /** Явка `dd.MM HH:mm` (пояс отображения §0.1). */
    val startText: String,
    val endText: String,
    val endTextWithDate: String,
    val durationText: String,
    /** «№N первая — последняя» или «Тип — Станция»; пусто — нет строки. */
    val summary: String,
    val isFuture: Boolean,
    val isTransition: Boolean,
    /** Ключи значков в порядке §4.5 (без статуса синхронизации). */
    val icons: List<String>,
    val isSynchronized: Boolean,
    val isFavorite: Boolean,
    /** «Расчёт за смену»: «1 234,56 ₽» или пусто (ошибка расчёта — суммы нет). */
    val paymentText: String,
    // ── Развёрнутый вид (§9.0.1) ──
    /** «серия №номер» по локомотивам. */
    val locoLines: List<String>,
    /** «№N первая — последняя» по поездам. */
    val trainLines: List<String>,
    /** «№N откуда — куда» по пассажирам. */
    val passengerLines: List<String>,
    /** «тип — станция» по прочей работе. */
    val otherWorkLines: List<String>,
    val notesText: String,
)

/**
 * Строка общего списка с учётом треёв «поездка с отдыхом в ПО» (§9.0.1).
 * kind: "route" | "tripHeader" | "tripConnector".
 */
data class AllRouteRowUi(
    val kind: String,
    val card: AllRouteCardUi? = null,
    /** kind = tripHeader: «Поездка с отдыхом · ЧЧ:ММ» / «Поездка с N отдыхами · ЧЧ:ММ». */
    val title: String = "",
    /** kind = tripConnector: станция коннектора (может быть пустой). */
    val connectorStation: String = "",
    /** kind = tripConnector: длительность «Отдых в ПО · {станция}» + ЧЧ:ММ. */
    val connectorDurationText: String = "",
    /** Индекс трея (−1 — строка вне трея). */
    val groupIndex: Int = -1,
)

/** Полное состояние экрана «Все маршруты». */
data class AllRoutesUi(
    val isReady: Boolean = false,
    /** Полноэкранный спиннер только первой загрузки. */
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val monthTitle: String = "",
    val yearTitle: String = "",
    val hasPrevMonth: Boolean = false,
    val hasNextMonth: Boolean = false,
    val monthOptions: List<Int> = emptyList(),
    val yearOptions: List<Int> = emptyList(),
    /** Число маршрутов месяца (без фильтра). */
    val routesCount: Int = 0,
    /** «Отработано за месяц» HH:MM или десятичный; пусто — пилюля скрыта. */
    val monthWorkedTimeText: String = "",
    val rows: List<AllRouteRowUi> = emptyList(),
    /** Имена выбранных фильтров (RouteFilter.name). */
    val selectedFilters: List<String> = listOf(RouteFilter.ALL.name),
    val sortOption: SortOption = SortOption.DATE_DESC,
    val isExpandedView: Boolean = false,
    val showTurnaroundRest: Boolean = true,
    val isSelectionMode: Boolean = false,
    val selectedCount: Int = 0,
    /** Все видимые (после фильтров) выделены — кнопка «Все»/«Снять». */
    val allVisibleSelected: Boolean = false,
    /** Все выбранные в избранном — иконка нижней панели. */
    val selectedAllFavorite: Boolean = false,
    /** Красная иконка сети в топбаре (тихий фоновый sync не достучался). */
    val isOffline: Boolean = false,
    val isBackgroundSyncing: Boolean = false,
)

/** Данные для share-sheet (семантика Android `ShareLinkData`). */
data class SharePayloadUi(
    val text: String,
    val subject: String,
)

/**
 * Решение по открытию формы (новый маршрут / копия) после гейта подписки —
 * как `NewRouteIosDecision`, но с параметрами формы. type: "open" | "limit" | "trial" | "error".
 */
data class AllRoutesFormDecision(
    val type: String,
    val basicId: String?,
    val isMakeCopy: Boolean,
    val freeRoutesLeft: Int,
    val isAuthorized: Boolean,
)

// ═════════════════════════════════════════════════════════════════════════════
// Общие форматтеры (§0.1) — как в HomeScreenIosViewModel.
// ═════════════════════════════════════════════════════════════════════════════

internal object Fmt {
    const val HOUR = 3_600_000L
    const val DAY = 24L * HOUR

    fun two(n: Int): String = if (n < 10) "0$n" else n.toString()

    /** `dd.MM HH:mm`. */
    fun dateMiniTime(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)} ${two(t.hour)}:${two(t.minute)}"
    }

    /** `HH:mm`. */
    fun time(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.hour)}:${two(t.minute)}"
    }

    /** `dd.MM.yy`. */
    fun date(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)}.${two(t.year % 100)}"
    }

    /** `dd.MM.yyyy`. */
    fun dateFull(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)}.${t.year}"
    }

    /** `dd.MM.yy HH:mm` — формат поискового индекса (§10.1). */
    fun dateTimeShort(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)}.${two(t.year % 100)} ${two(t.hour)}:${two(t.minute)}"
    }

    /** `dd.MM.yyyy HH:mm`. */
    fun dateTimeFull(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)}.${t.year} ${two(t.hour)}:${two(t.minute)}"
    }

    /**
     * Длительность как `ConverterLongToTime` (§0.1): `ЧЧ:ММ` (часы не ограничены 24,
     * отрицательное → `00:00`, null → пробелы) или десятичные часы `Ч,ММ`.
     */
    fun duration(ms: Long?, isDecimal: Boolean): String {
        if (ms == null) return if (isDecimal) "" else "          "
        if (isDecimal) {
            val totalMinutes = ms / 60_000L
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
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

    fun daysWord(n: Long): String {
        val nn = n % 100
        val n1 = n % 10
        return when {
            nn in 11..14 -> "дней"
            n1 == 1L -> "день"
            n1 in 2..4 -> "дня"
            else -> "дней"
        }
    }

    /** Целое без дробной части, дробное — как есть (у.д. §9.1). */
    fun number(value: String?): String = value.orEmpty()
}

// ═════════════════════════════════════════════════════════════════════════════
// Итог оплаты за смену — порт `RouteSalaryCalculator.computeRouteTotalPayment`
// (features/route недоступен из iosApp; суммируем те же компоненты
// `SalaryCalculationHelper`, что и Android).
// ═════════════════════════════════════════════════════════════════════════════

internal suspend fun computeRouteTotalPayment(
    route: Route,
    userSettings: UserSettings,
    salarySetting: SalarySetting,
    candidateRoutes: List<Route>,
): Double? {
    if (route.getPureWorkTime() == null) return null

    val helper = SalaryCalculationHelper(
        userSettings = userSettings,
        salarySetting = salarySetting,
        allRoutes = listOf(route),
    )

    return coroutineScope {
        val tariff = async {
            helper.getMoneyAtWorkTimeAtTariffSingleRoute().first().coerceAtLeast(0.0)
        }
        val night = async { helper.getMoneyAtNightTimeFlow().first() }
        val zonal = async { helper.getMoneyZonalSurchargeFlow().first() }
        val passenger = async { helper.getMoneyAtPassengerFlow().first() }
        val passengerOutside = async { helper.getMoneyAtPassengerOutsideWorkFlow().first() }
        val passengerWaiting = async { helper.getMoneyAtPassengerWaitingFlow().first() }
        val singleLocomotive = async { helper.getMoneyAtSingleLocomotiveFlow().first() }
        val holiday = async { helper.getMoneyAtHolidayFlow().first() }
        val servicePhase = async {
            helper.getMoneyListSurchargeExtendedServicePhaseFlow().first().sum()
        }
        val heavy = async { helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum() }
        val longTrain = async { helper.getMoneyListSurchargeLongTrainsFlow().first().sum() }
        val doubledFirst = async {
            helper.getMoneyDoubledTrainFirstSurchargeFlow(listOf(route)).first()
        }
        val doubledSecond = async {
            helper.getMoneyDoubledTrainSecondSurchargeFlow(listOf(route)).first()
        }
        val qualification = async { helper.getMoneyAtQualificationClassFlow().first() }
        val nordic = async { helper.getMoneyNordicSurcharge().first() }
        val district = async { helper.getMoneyDistrictSurcharge().first() }
        val harmfulness = async { helper.getMoneyHarmfulnessFlow().first() }
        val other = async { helper.getMoneyOtherSurchargeFlow().first() }
        val businessTrip = async { helper.getMoneyBusinessTripFlow().first() }

        val isPassengerTrain = route.usesOnePersonPassengerRate()
        val onePerson = async {
            if (isPassengerTrain) {
                helper.getMoneyOnePersonOperationPassengerTrainFlow().first()
            } else {
                helper.getMoneyOnePersonOperationFlow().first()
            }
        }

        // Переотдых: доплата 2/3 тарифа за часы сверх нормы отдыха в ПО
        // ближайшего по явке предыдущего маршрута (как в FormViewModel).
        val overRestMoney = route.overRestPayment(
            previous = candidateRoutes.previousRouteFor(route),
            minTimeRest = userSettings.minTimeRestPointOfTurnover,
            tariffRate = userSettings.selectMonthOfYear.tariffRate,
        ).money

        tariff.await() + night.await() + zonal.await() + passenger.await() +
            passengerWaiting.await() +
            passengerOutside.await() + singleLocomotive.await() + holiday.await() +
            servicePhase.await() +
            heavy.await() + longTrain.await() + doubledFirst.await() + doubledSecond.await() +
            onePerson.await() + qualification.await() + nordic.await() + district.await() +
            harmfulness.await() + other.await() + overRestMoney + businessTrip.await()
    }
}

/** «1 234,56 ₽»; null (ошибка/нет времени работы) → пустая строка (§9.0.1). */
internal fun paymentText(total: Double?, currency: String): String =
    total?.toMoneyString(currency) ?: ""

// ═════════════════════════════════════════════════════════════════════════════
// Share payload — порт `ShareLinkData.fromRoute/fromRoutes` (без Android-Intent).
// Дата в тексте — как Android `SimpleDateFormat` в поясе устройства.
// ═════════════════════════════════════════════════════════════════════════════

internal fun buildSharePayload(links: List<Pair<Route, String>>): SharePayloadUi {
    // BUG-32 (паритет с Android): даты текста шаринга форматируются в поясе
    // устройства (SimpleDateFormat на Android), не в поясе отображения.
    val deviceTz = TimeZone.currentSystemDefault()
    fun routeDateText(route: Route): String? = route.basicData.timeStartWork?.let {
        Fmt.dateTimeFull(it, deviceTz)
    }

    if (links.size == 1) {
        val (route, url) = links.first()
        val text = buildString {
            append("Маршрут из приложения «Машинист» 🚂")
            append("\n")
            routeDateText(route)?.let { append(" от $it") }
            append("\n")
            val stations = route.trains
                .flatMap { it.stations }
                .sortedBy { it.orderIndex }
            val firstStation = stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
            val lastStation = stations.lastOrNull()?.stationName?.takeIf { it.isNotBlank() }
            if (firstStation != null && lastStation != null && firstStation != lastStation) {
                append(", $firstStation — $lastStation")
            }
            append("\n\n")
            append("Чтобы открыть маршрут, нажмите на ссылку внизу")
            append("\n\n")
            append(url)
        }
        val subject = routeDateText(route)?.let { "Маршрут от $it" } ?: "Маршрут"
        return SharePayloadUi(text = text, subject = subject)
    }
    val text = buildString {
        append("Маршруты из приложения «Машинист» 🚂")
        append("\n\n")
        links.forEach { (route, url) ->
            val date = routeDateText(route)
            append(if (date != null) "Маршрут от $date" else "Маршрут")
            append("\n")
            append(url)
            append("\n\n")
        }
        append("Чтобы открыть маршрут, нажмите на нужную ссылку")
    }
    return SharePayloadUi(text = text, subject = "Маршруты (${links.size})")
}

// ═════════════════════════════════════════════════════════════════════════════
// Трей «поездка с отдыхом в ПО» — порт `util/TurnaroundRest.kt` и
// `util/RouteEndStation.kt` (features/route недоступен из iosApp).
// ═════════════════════════════════════════════════════════════════════════════

internal data class TurnaroundRestLink(
    val station: String,
    val durationMs: Long,
)

/**
 * Коннектор «отдых в ПО» между СМЕЖНЫМИ в списке маршрутами (§9.0.1).
 * Пара засчитывается: у более раннего по явке стоит `restPointOfTurnover`,
 * у обоих есть явка/сдача, явка позднего строго позже сдачи раннего.
 */
internal fun turnaroundRestBetween(a: Route, b: Route): TurnaroundRestLink? {
    val ta = a.basicData.timeStartWork ?: return null
    val tb = b.basicData.timeStartWork ?: return null
    val outbound = if (ta <= tb) a else b
    val ret = if (ta <= tb) b else a

    if (!outbound.basicData.restPointOfTurnover) return null

    val returnStart = ret.basicData.timeStartWork ?: return null
    val outboundEnd = outbound.basicData.timeEndWork ?: return null
    val gap = returnStart - outboundEnd
    if (gap <= 0L) return null

    val station = routeEndStation(outbound)
        ?: ret.trains.firstOrNull()?.stations
            ?.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
        ?: ""

    return TurnaroundRestLink(station = station, durationMs = gap)
}

/** Станция, на которой маршрут фактически закончился (порт `RouteEndStation.kt`). */
internal fun routeEndStation(route: Route): String? {
    data class Candidate(val name: String, val time: Long?, val untimedRank: Int) {
        val rank get() = if (time != null) 0 else untimedRank
    }

    val candidates = buildList {
        val trains = route.trains.sortedWith(
            compareBy(nullsLast()) { it.stations.firstOrNull()?.timeDeparture }
        )
        trains.forEach { train ->
            val last = train.stations.lastOrNull() ?: return@forEach
            val name = last.stationName?.takeIf { it.isNotBlank() } ?: return@forEach
            add(Candidate(name, last.timeArrival ?: last.timeDeparture, 1))
        }
        route.passengers.forEach { passenger ->
            val name = passenger.stationArrival?.takeIf { it.isNotBlank() } ?: return@forEach
            add(Candidate(name, passenger.timeArrival ?: passenger.timeDeparture, 2))
        }
    }
    if (candidates.isEmpty()) return null

    return candidates.reduce { best, c ->
        when {
            c.rank != best.rank -> if (c.rank > best.rank) c else best
            (c.time ?: 0L) >= (best.time ?: 0L) -> c
            else -> best
        }
    }.name
}
