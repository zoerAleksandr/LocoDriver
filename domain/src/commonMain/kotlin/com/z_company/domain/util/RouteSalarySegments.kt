package com.z_company.domain.util

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.Route
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
    val freightOnePersonIntervals = if (basicData.isOnePersonOperation) {
        trainCategoryIntervals(workInterval, isPassengerTrain = false)
    } else emptyList()
    val passengerOnePersonIntervals = if (basicData.isOnePersonOperation) {
        trainCategoryIntervals(workInterval, isPassengerTrain = true)
    } else emptyList()
    val doubledFirstIntervals = trainIntervals(workInterval) { it.doubledTrain?.isFirst == true }
    val doubledSecondIntervals = trainIntervals(workInterval) { it.doubledTrain?.isFirst == false }
    val heavyLongDistanceIntervals = trainIntervals(workInterval) { train ->
        (train.weight?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0) > 6000.0 &&
                (train.axle?.trim()?.replace(',', '.')?.toDoubleOrNull()
                    ?.takeIf { it.isFinite() && it % 1.0 == 0.0 }
                    ?.toInt() ?: 0) >= 350
    }

    val conditions = buildMap<AccrualCondition, Iterable<TimeInterval>> {
        if (nightIntervals.isNotEmpty()) put(AccrualCondition.NIGHT, nightIntervals)
        if (holidayIntervals.isNotEmpty()) put(AccrualCondition.HOLIDAY, holidayIntervals)
        if (passengerIntervals.isNotEmpty()) put(AccrualCondition.PASSENGER, passengerIntervals)
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

private fun Route.trainCategoryIntervals(
    workInterval: TimeInterval,
    isPassengerTrain: Boolean,
): List<TimeInterval> {
    fun isPassenger(number: String?): Boolean {
        val parsed = number?.trim()?.toIntOrNull() ?: return false
        return passengerTrainNumberList.any { parsed in it }
    }

    // Продуктовое правило: если поезд не указан, используем грузовую ставку
    // работы в одно лицо. Пассажирскую ставку без явного поезда не назначаем.
    if (trains.isEmpty()) {
        return if (isPassengerTrain) emptyList() else listOf(workInterval)
    }

    val selected = trains.filter { isPassenger(it.number) == isPassengerTrain }
    if (selected.isEmpty()) return emptyList()
    val timed = selected.mapNotNull { train ->
        val start = train.stations.firstOrNull()?.timeDeparture
        val end = train.stations.lastOrNull()?.timeArrival
        validInterval(start, end)?.intersect(workInterval)
    }.mergeTimeIntervals()
    if (timed.isNotEmpty()) return timed

    val routeHasOnlyThisCategory = trains.all { isPassenger(it.number) == isPassengerTrain }
    return if (routeHasOnlyThisCategory) listOf(workInterval) else emptyList()
}

private fun validInterval(first: Long?, second: Long?): TimeInterval? {
    if (first == null || second == null) return null
    val start = minOf(first, second)
    val end = maxOf(first, second)
    return if (end > start) TimeInterval(start, end) else null
}
