package com.z_company.domain.util

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.clipToMonth
import com.z_company.domain.entities.route.UtilsForEntities.passengerTrainNumberList
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

data class NightWindow(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val offsetFromMoscowMillis: Long,
) {
    init {
        require(startHour in 0..23 && endHour in 0..23)
        require(startMinute in 0..59 && endMinute in 0..59)
    }
}

/** Преобразует фактические данные маршрута в KMP-сегменты расчёта. */
fun Route.buildSalarySegments(
    monthOfYear: MonthOfYear,
    context: TimeCalculationContext,
    initialTariffRatePerHour: Double,
    tariffChanges: Iterable<TariffChange> = emptyList(),
    nightWindow: NightWindow? = null,
): List<SalarySegment> {
    val (start, end) = clipToMonth(monthOfYear, context) ?: return emptyList()
    val workInterval = TimeInterval(start, end)
    val breakInterval = validInterval(
        basicData.timeStartBreak,
        basicData.timeEndBreak,
    )?.intersect(workInterval)
    val passengerIntervals = passengers.mapNotNull { passenger ->
        validInterval(passenger.timeDeparture, passenger.timeArrival)?.intersect(workInterval)
    }
    // «Ожидание следования пассажиром»: остаток рабочего времени, СТРОГО
    // примыкающий концом к отправлению пассажиром внутри смены (сдал локомотив →
    // ждёт → поехал пассажиром). Считается только при наличии следования
    // пассажиром — в чисто грузовом маршруте ожидание отсутствует.
    val passengerWaitingIntervals = passengerWaitingIntervals(
        workInterval = workInterval,
        breakInterval = breakInterval,
    )
    val nightIntervals = nightWindow?.let { window ->
        CalculateNightTime.getNightIntervals(
            startMillis = start,
            endMillis = end,
            hourStart = window.startHour,
            minuteStart = window.startMinute,
            hourEnd = window.endHour,
            minuteEnd = window.endMinute,
            offsetInMoscow = window.offsetFromMoscowMillis,
        ).map { (nightStart, nightEnd) -> TimeInterval(nightStart, nightEnd) }
    }.orEmpty()
    val holidayIntervals = monthOfYear.days.asSequence()
        .filter { day ->
            day.tag == TagForDay.HOLIDAY ||
                    (day.isReleaseDay && day.releaseType == ReleaseType.DayOff)
        }
        .mapNotNull { day ->
            val date = LocalDate(monthOfYear.year, monthOfYear.month + 1, day.dayOfMonth)
            TimeInterval(
                startMillis = date.atStartOfDayIn(context.localTZ).toEpochMilliseconds(),
                endMillis = date.plus(1, DateTimeUnit.DAY)
                    .atStartOfDayIn(context.localTZ).toEpochMilliseconds(),
            ).intersect(workInterval)
        }
        .toList()
    val onePersonIntervals = onePersonOperationIntervals(
        workInterval = workInterval,
        breakInterval = breakInterval,
        passengerIntervals = passengerIntervals,
        passengerWaitingIntervals = passengerWaitingIntervals,
    )
    val onePersonIsPassenger = hasPassengerTrain()
    val freightOnePersonIntervals = if (onePersonIsPassenger) emptyList() else onePersonIntervals
    val passengerOnePersonIntervals = if (onePersonIsPassenger) onePersonIntervals else emptyList()
    val doubledFirstIntervals = trainIntervals(workInterval) { it.doubledTrain?.isFirst == true }
    val doubledSecondIntervals = trainIntervals(workInterval) { it.doubledTrain?.isFirst == false }
    val reserveIntervals = trainIntervals(workInterval) { train ->
        when (train.number?.toIntOrNull()) {
            in 4001..4148, in 4151..4188, in 4191..4198, in 4201..4228,
            in 4231..4258, in 4261..4298, in 4301..4398, in 4401..4698,
            in 4701..4778, in 4801..4898 -> true
            else -> false
        }
    }
    val heavyLongDistanceIntervals = trainIntervals(workInterval) { train ->
        (train.weight?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0) > 6000.0 &&
                (train.axle?.trim()?.replace(',', '.')?.toDoubleOrNull()
                    ?.takeIf { it.isFinite() && it % 1.0 == 0.0 }
                    ?.toInt() ?: 0) > 350
    }

    val conditions = buildMap<AccrualCondition, Iterable<TimeInterval>> {
        if (nightIntervals.isNotEmpty()) put(AccrualCondition.NIGHT, nightIntervals)
        if (holidayIntervals.isNotEmpty()) put(AccrualCondition.HOLIDAY, holidayIntervals)
        if (passengerIntervals.isNotEmpty()) put(AccrualCondition.PASSENGER, passengerIntervals)
        if (passengerWaitingIntervals.isNotEmpty()) {
            put(AccrualCondition.PASSENGER_WAITING, passengerWaitingIntervals)
        }
        if (freightOnePersonIntervals.isNotEmpty()) {
            put(AccrualCondition.ONE_PERSON_FREIGHT, freightOnePersonIntervals)
        }
        if (passengerOnePersonIntervals.isNotEmpty()) {
            put(AccrualCondition.ONE_PERSON_PASSENGER, passengerOnePersonIntervals)
        }
        val allOnePersonIntervals = freightOnePersonIntervals + passengerOnePersonIntervals
        if (allOnePersonIntervals.isNotEmpty()) put(AccrualCondition.ONE_PERSON, allOnePersonIntervals)
        if (doubledFirstIntervals.isNotEmpty()) {
            put(AccrualCondition.DOUBLED_TRAIN_FIRST, doubledFirstIntervals)
        }
        if (doubledSecondIntervals.isNotEmpty()) {
            put(AccrualCondition.DOUBLED_TRAIN_SECOND, doubledSecondIntervals)
        }
        val allDoubledIntervals = doubledFirstIntervals + doubledSecondIntervals
        if (allDoubledIntervals.isNotEmpty()) {
            put(AccrualCondition.DOUBLED_TRAIN, allDoubledIntervals)
        }
        if (reserveIntervals.isNotEmpty()) put(AccrualCondition.RESERVE, reserveIntervals)
        if (heavyLongDistanceIntervals.isNotEmpty()) {
            put(AccrualCondition.HEAVY_LONG_DISTANCE_TRAIN, heavyLongDistanceIntervals)
        }
    }
    return buildSalarySegments(
        workIntervals = listOf(workInterval),
        unpaidIntervals = listOfNotNull(breakInterval),
        initialTariffRatePerHour = initialTariffRatePerHour,
        tariffChanges = tariffChanges,
        conditionIntervals = conditions,
    )
}

/**
 * Строит непересекающиеся сегменты поездной доплаты по настраиваемым порогам.
 * Если интервалы поездов разных диапазонов пересекаются, в каждый момент
 * применяется только диапазон с наибольшим подходящим порогом.
 */
fun Route.buildTieredTrainSurchargeSegments(
    monthOfYear: MonthOfYear,
    context: TimeCalculationContext,
    initialTariffRatePerHour: Double,
    thresholds: List<Int>,
    condition: AccrualCondition,
    // Порог — строгая граница: ступень действует при значении БОЛЬШЕ порога,
    // равенство порогу доплату не включает (тяжеловесный, длинносоставный,
    // удлинённое плечо — одно правило на Android и PWA).
    thresholdIsInclusive: Boolean = false,
    tariffChanges: Iterable<TariffChange> = emptyList(),
    valueOf: (Train) -> Int?,
): List<List<SalarySegment>> {
    require(thresholds.all { it > 0 }) { "Train surcharge thresholds must be positive" }
    require(thresholds.zipWithNext().all { (first, second) -> first < second }) {
        "Train surcharge thresholds must be unique and strictly increasing"
    }
    if (thresholds.isEmpty()) return emptyList()

    val workInterval = clipToMonth(monthOfYear, context)
        ?.let { (start, end) -> TimeInterval(start, end) }
        ?: return List(thresholds.size) { emptyList() }
    val baseSegments = buildSalarySegments(
        monthOfYear = monthOfYear,
        context = context,
        initialTariffRatePerHour = initialTariffRatePerHour,
        tariffChanges = tariffChanges,
    )
    val intervalsByTier = thresholds.indices.map { index ->
        val lower = thresholds[index]
        val upper = thresholds.getOrNull(index + 1)
        trainIntervals(workInterval) { train ->
            val value = valueOf(train) ?: return@trainIntervals false
            val reachesLowerBound = if (thresholdIsInclusive) value >= lower else value > lower
            val staysBelowNextTier = upper == null ||
                    if (thresholdIsInclusive) value < upper else value <= upper
            reachesLowerBound && staysBelowNextTier
        }
    }
    val selectedByTier = MutableList(thresholds.size) { emptyList<TimeInterval>() }
    var intervalsClaimedByHigherTiers = emptyList<TimeInterval>()
    for (index in thresholds.indices.reversed()) {
        val selected = intervalsByTier[index]
            .flatMap { it.subtractAll(intervalsClaimedByHigherTiers) }
            .mergeTimeIntervals()
        selectedByTier[index] = selected
        intervalsClaimedByHigherTiers =
            (intervalsClaimedByHigherTiers + selected).mergeTimeIntervals()
    }

    return selectedByTier.map { activeIntervals ->
        baseSegments.flatMap { segment ->
            activeIntervals.mapNotNull { active ->
                segment.interval.intersect(active)?.let { intersection ->
                    segment.copy(
                        interval = intersection,
                        conditions = segment.conditions + condition,
                    )
                }
            }
        }
    }
}

/**
 * Интервалы «ожидания следования пассажиром» — непокрытый остаток рабочего
 * времени, примыкающий концом к моменту отправления пассажиром (внутри смены).
 *
 * Покрытием считаются: работа на локомотиве (от начала приёмки до конца сдачи),
 * следование с поездом (первое отправление — последнее прибытие) и следование
 * пассажиром. Перерыв исключается (он не оплачивается). Из полученного остатка
 * берутся только куски, чей конец совпадает с отправлением пассажиром — то есть
 * время «строго перед» посадкой пассажиром. Крайние остатки (подготовительно-
 * заключительное время) и остатки без последующего следования пассажиром
 * ожиданием не считаются и остаются обычной работой по тарифу.
 */
private fun Route.passengerWaitingIntervals(
    workInterval: TimeInterval,
    breakInterval: TimeInterval?,
): List<TimeInterval> {
    // Отправления пассажиром внутри смены — границы, к которым примыкает ожидание.
    val passengerWithinWork = passengers
        .filterNot { it.isWorkStartByArrival }
        .mapNotNull { validInterval(it.timeDeparture, it.timeArrival)?.intersect(workInterval) }
    if (passengerWithinWork.isEmpty()) return emptyList()
    val passengerDepartures = passengerWithinWork.map { it.startMillis }.toSet()

    // Работа на локомотиве: от начала приёмки до конца сдачи; если заданы не все
    // отметки — используем доступные подынтервалы приёмки/сдачи по отдельности.
    val locomotiveIntervals = locomotives.flatMap { loco ->
        listOfNotNull(
            validInterval(loco.timeStartOfAcceptance, loco.timeEndOfDelivery),
            validInterval(loco.timeStartOfAcceptance, loco.timeEndOfAcceptance),
            validInterval(loco.timeStartOfDelivery, loco.timeEndOfDelivery),
        )
    }.mapNotNull { it.intersect(workInterval) }

    val trainFollowingIntervals = trains.mapNotNull { train ->
        validInterval(
            train.stations.firstOrNull()?.timeDeparture,
            train.stations.lastOrNull()?.timeArrival,
        )?.intersect(workInterval)
    }

    val coveredIntervals = (locomotiveIntervals + trainFollowingIntervals + passengerWithinWork)
        .mergeTimeIntervals()
    val exclusions = coveredIntervals + listOfNotNull(breakInterval)

    return workInterval.subtractAll(exclusions)
        .filter { it.endMillis in passengerDepartures }
}

private fun Route.trainIntervals(
    workInterval: TimeInterval,
    predicate: (com.z_company.domain.entities.route.Train) -> Boolean,
): List<TimeInterval> = trains.asSequence()
    .filter(predicate)
    .mapNotNull { train ->
        validInterval(
            train.stations.firstOrNull()?.timeDeparture,
            train.stations.lastOrNull()?.timeArrival,
        )?.intersect(workInterval)
    }
    .toList()
    .mergeTimeIntervals()

/**
 * Время работы в одно лицо (153L) в пределах месяца.
 *
 * Доплата начисляется «по маршруту машиниста» за фактически отработанное время
 * без помощника — это вся смена от явки до сдачи, а не только интервал поезда.
 * Из смены исключаются перерыв, следование пассажиром и ожидание следования
 * пассажиром (018M): в это время машинист не работает на локомотиве.
 *
 * @param passengerTrain `true` — время по пассажирской ставке, `false` — по грузовой.
 * Категория общая на всю смену: см. [hasPassengerTrain].
 */
fun Route.onePersonOperationTime(
    monthOfYear: MonthOfYear,
    context: TimeCalculationContext,
    passengerTrain: Boolean,
): Long {
    if (!basicData.isOnePersonOperation || hasPassengerTrain() != passengerTrain) return 0L
    val (start, end) = clipToMonth(monthOfYear, context) ?: return 0L
    val workInterval = TimeInterval(start, end)
    val breakInterval = validInterval(
        basicData.timeStartBreak,
        basicData.timeEndBreak,
    )?.intersect(workInterval)
    val passengerIntervals = passengers.mapNotNull { passenger ->
        validInterval(passenger.timeDeparture, passenger.timeArrival)?.intersect(workInterval)
    }
    return onePersonOperationIntervals(
        workInterval = workInterval,
        breakInterval = breakInterval,
        passengerIntervals = passengerIntervals,
        passengerWaitingIntervals = passengerWaitingIntervals(workInterval, breakInterval),
    ).sumOf { it.durationMillis }
}

/**
 * Категория доплаты в одно лицо определяется на всю смену: достаточно одного
 * пассажирского (пригородного) поезда, чтобы вся смена шла по пассажирской
 * ставке. Без пассажирских поездов (в т. ч. без указанного поезда) — грузовая.
 */
private fun Route.hasPassengerTrain(): Boolean = trains.any { train ->
    val parsed = train.number?.trim()?.toIntOrNull() ?: return@any false
    passengerTrainNumberList.any { parsed in it }
}

private fun Route.onePersonOperationIntervals(
    workInterval: TimeInterval,
    breakInterval: TimeInterval?,
    passengerIntervals: List<TimeInterval>,
    passengerWaitingIntervals: List<TimeInterval>,
): List<TimeInterval> {
    if (!basicData.isOnePersonOperation) return emptyList()
    val exclusions = listOfNotNull(breakInterval) + passengerIntervals + passengerWaitingIntervals
    return workInterval.subtractAll(exclusions)
}

private fun validInterval(first: Long?, second: Long?): TimeInterval? {
    if (first == null || second == null) return null
    val start = minOf(first, second)
    val end = maxOf(first, second)
    return if (end > start) TimeInterval(start, end) else null
}
