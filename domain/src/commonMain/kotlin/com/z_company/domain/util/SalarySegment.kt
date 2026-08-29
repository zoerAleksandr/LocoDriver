package com.z_company.domain.util

enum class AccrualCondition {
    REGULAR,
    NIGHT,
    HOLIDAY,
    BUSINESS_TRIP,
    OVERTIME,
    HARMFUL,
    ONE_PERSON,
    ONE_PERSON_FREIGHT,
    ONE_PERSON_PASSENGER,
    PASSENGER,
    RESERVE,
    HEAVY_TRAIN,
    HEAVY_LONG_DISTANCE_TRAIN,
    LONG_TRAIN,
    DOUBLED_TRAIN,
    DOUBLED_TRAIN_FIRST,
    DOUBLED_TRAIN_SECOND,
}

data class SalarySegment(
    val interval: TimeInterval,
    val tariffRatePerHour: Double,
    val conditions: Set<AccrualCondition> = setOf(AccrualCondition.REGULAR),
) {
    init {
        require(tariffRatePerHour.isFinite() && tariffRatePerHour >= 0.0) {
            "Tariff rate must be finite and non-negative"
        }
    }

    val tariffMoney: Double
        get() = interval.durationMillis.toDouble() * tariffRatePerHour / MILLIS_PER_HOUR

    fun splitAt(boundariesMillis: Iterable<Long>): List<SalarySegment> =
        interval.splitAt(boundariesMillis).map { copy(interval = it) }

    fun subtract(exclusions: Iterable<TimeInterval>): List<SalarySegment> =
        interval.subtractAll(exclusions).map { copy(interval = it) }

    companion object {
        private const val MILLIS_PER_HOUR = 3_600_000.0
    }
}

/**
 * Разрезает сегмент по границам действия условия и добавляет условие только
 * пересекающимся частям. Интервалы трактуются как полуинтервалы [start, end).
 */
fun SalarySegment.applyCondition(
    condition: AccrualCondition,
    activeIntervals: Iterable<TimeInterval>,
): List<SalarySegment> {
    val activeParts = activeIntervals
        .mapNotNull(interval::intersect)
        .mergeTimeIntervals()
    if (activeParts.isEmpty()) return listOf(this)

    val boundaries = activeParts.flatMap { listOf(it.startMillis, it.endMillis) }
    return splitAt(boundaries).map { part ->
        val isActive = activeParts.any { it.intersect(part.interval) != null }
        if (isActive) part.copy(conditions = part.conditions + condition) else part
    }
}

fun Iterable<SalarySegment>.applyCondition(
    condition: AccrualCondition,
    activeIntervals: Iterable<TimeInterval>,
): List<SalarySegment> = flatMap { it.applyCondition(condition, activeIntervals) }

/**
 * Единый конвейер построения непересекающихся расчётных сегментов.
 * Сначала исключает неоплачиваемые интервалы, затем режет по тарифам и границам
 * условий. Все операции используют полуинтервалы [start, end).
 */
fun buildSalarySegments(
    workIntervals: Iterable<TimeInterval>,
    unpaidIntervals: Iterable<TimeInterval> = emptyList(),
    initialTariffRatePerHour: Double,
    tariffChanges: Iterable<TariffChange> = emptyList(),
    conditionIntervals: Map<AccrualCondition, Iterable<TimeInterval>> = emptyMap(),
): List<SalarySegment> {
    val work = workIntervals
        .filterNot(TimeInterval::isEmpty)
        .sortedBy(TimeInterval::startMillis)
    work.zipWithNext().forEach { (previous, next) ->
        require(previous.endMillis <= next.startMillis) { "Work intervals must not overlap" }
    }
    var segments = work.flatMap { interval ->
        interval.subtractAll(unpaidIntervals).flatMap { payable ->
            payable.applyTariffChanges(initialTariffRatePerHour, tariffChanges)
        }
    }
    conditionIntervals.forEach { (condition, intervals) ->
        segments = segments.applyCondition(condition, intervals)
    }
    return segments.sortedBy { it.interval.startMillis }
}

data class TariffChange(
    val effectiveAtMillis: Long,
    val tariffRatePerHour: Double,
) {
    init {
        require(tariffRatePerHour.isFinite() && tariffRatePerHour >= 0.0) {
            "Tariff rate must be finite and non-negative"
        }
    }
}

fun TimeInterval.applyTariffChanges(
    initialTariffRatePerHour: Double,
    changes: Iterable<TariffChange>,
    conditions: Set<AccrualCondition> = setOf(AccrualCondition.REGULAR),
): List<SalarySegment> {
    require(initialTariffRatePerHour.isFinite() && initialTariffRatePerHour >= 0.0)
    val orderedChanges = changes
        .filter { it.effectiveAtMillis < endMillis }
        .sortedBy(TariffChange::effectiveAtMillis)
    val boundaries = orderedChanges.map(TariffChange::effectiveAtMillis)
    return splitAt(boundaries).map { part ->
        val rate = orderedChanges
            .lastOrNull { it.effectiveAtMillis <= part.startMillis }
            ?.tariffRatePerHour
            ?: initialTariffRatePerHour
        SalarySegment(part, rate, conditions)
    }
}
