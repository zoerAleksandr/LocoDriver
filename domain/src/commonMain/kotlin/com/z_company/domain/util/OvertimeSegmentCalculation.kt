package com.z_company.domain.util

data class OvertimeBreakdown(
    val overtimeSegments: List<SalarySegment>,
    val halfRateSegments: List<SalarySegment>,
    val fullRateSegments: List<SalarySegment>,
    val ordinaryTariffMoney: Double,
    val halfRateExtraMoney: Double,
    val fullRateExtraMoney: Double,
) {
    val totalMoney: Double
        get() = ordinaryTariffMoney + halfRateExtraMoney + fullRateExtraMoney
}

data class OvertimePremiumDurations(
    val halfRateMillis: Long,
    val fullRateMillis: Long,
) {
    init {
        require(halfRateMillis >= 0L && fullRateMillis >= 0L)
    }

    val totalMillis: Long
        get() = halfRateMillis + fullRateMillis
}

/**
 * Распределяет длительность повышающей части сверхурочных.
 * С сентября 2026 первые два часа определяются отдельно для каждой смены, а
 * после 120-го часа календарного года вся оставшаяся переработка идёт в двойную
 * категорию. Ограничение 240 часов здесь намеренно отсутствует.
 */
fun calculateOvertimePremiumDurations(
    overtimeByShiftMillis: Iterable<Long>,
    year: Int,
    zeroBasedMonth: Int,
    annualOvertimeBeforePeriodMillis: Long = 0L,
): OvertimePremiumDurations {
    val overtimeByShift = overtimeByShiftMillis.toList()
    require(overtimeByShift.all { it >= 0L }) { "Shift overtime must be non-negative" }
    require(annualOvertimeBeforePeriodMillis >= 0L) {
        "Annual overtime before period must be non-negative"
    }
    val total = overtimeByShift.sum()
    val law144Effective = year > 2026 || (year == 2026 && zeroBasedMonth >= 8)
    val candidateHalfRate = if (law144Effective) {
        overtimeByShift.sumOf { minOf(it, TWO_HOURS_MILLIS) }
    } else {
        val actualOvertimeShiftCount = overtimeByShift.count { it > 0L }
        minOf(total, actualOvertimeShiftCount.toLong() * TWO_HOURS_MILLIS)
    }
    val halfRate = if (law144Effective) {
        minOf(
            candidateHalfRate,
            (ANNUAL_DOUBLE_RATE_THRESHOLD_MILLIS - annualOvertimeBeforePeriodMillis)
                .coerceAtLeast(0L),
        )
    } else {
        candidateHalfRate
    }
    return OvertimePremiumDurations(
        halfRateMillis = halfRate,
        fullRateMillis = total - halfRate,
    )
}

private const val TWO_HOURS_MILLIS = 2L * 3_600_000L
private const val ANNUAL_DOUBLE_RATE_THRESHOLD_MILLIS = 120L * 3_600_000L

/**
 * Выбирает фактические сегменты переработки с конца расчётного периода.
 * Праздничные часы не потребляют требуемую длительность сверхурочных, поскольку
 * оплачиваются по отдельному основанию.
 */
fun selectLatestOvertimeSegments(
    workSegments: Iterable<SalarySegment>,
    overtimeDurationMillis: Long,
): List<SalarySegment> {
    require(overtimeDurationMillis >= 0L) { "Overtime duration must be non-negative" }
    val eligible = workSegments
        .filterNot { it.interval.isEmpty || AccrualCondition.HOLIDAY in it.conditions }
        .sortedBy { it.interval.startMillis }
        .also(::requireNonOverlapping)
    var remaining = minOf(
        overtimeDurationMillis,
        eligible.sumOf { it.interval.durationMillis },
    )
    if (remaining == 0L) return emptyList()

    val selectedReversed = mutableListOf<SalarySegment>()
    eligible.asReversed().forEach { segment ->
        if (remaining <= 0L) return@forEach
        val selectedDuration = minOf(remaining, segment.interval.durationMillis)
        selectedReversed += segment.copy(
            interval = TimeInterval(
                startMillis = segment.interval.endMillis - selectedDuration,
                endMillis = segment.interval.endMillis,
            ),
            conditions = segment.conditions + AccrualCondition.OVERTIME,
        )
        remaining -= selectedDuration
    }
    return selectedReversed.asReversed()
}

/**
 * Рассчитывает сверхурочные только по фактическим сегментам и действующим на них
 * условиям. Проценты передаются для конкретных причин начисления, поэтому ночная
 * или поездная надбавка не усредняется по остальному месяцу.
 *
 * Праздничные сегменты исключаются: они оплачиваются отдельно и не должны
 * одновременно попадать в сверхурочные.
 */
fun calculateOvertimeBreakdown(
    segments: Iterable<SalarySegment>,
    halfRateDurationMillis: Long,
    conditionPercents: Map<AccrualCondition, Double> = emptyMap(),
): OvertimeBreakdown {
    require(halfRateDurationMillis >= 0L) { "Half-rate duration must be non-negative" }
    conditionPercents.forEach { (condition, percent) ->
        require(percent.isFinite() && percent >= 0.0) {
            "Percent for $condition must be finite and non-negative"
        }
    }

    val overtimeSegments = segments
        .filterNot { it.interval.isEmpty || AccrualCondition.HOLIDAY in it.conditions }
        .sortedBy { it.interval.startMillis }
        .also(::requireNonOverlapping)
        .map { it.copy(conditions = it.conditions + AccrualCondition.OVERTIME) }

    var halfRateRemaining = minOf(
        halfRateDurationMillis,
        overtimeSegments.sumOf { it.interval.durationMillis },
    )
    val halfRateSegments = mutableListOf<SalarySegment>()
    val fullRateSegments = mutableListOf<SalarySegment>()
    overtimeSegments.forEach { segment ->
        when {
            halfRateRemaining <= 0L -> fullRateSegments += segment
            halfRateRemaining >= segment.interval.durationMillis -> {
                halfRateSegments += segment
                halfRateRemaining -= segment.interval.durationMillis
            }
            else -> {
                val boundary = segment.interval.startMillis + halfRateRemaining
                val parts = segment.splitAt(listOf(boundary))
                halfRateSegments += parts.first()
                fullRateSegments += parts.last()
                halfRateRemaining = 0L
            }
        }
    }

    fun applicableBaseMoney(segment: SalarySegment): Double {
        val percent = conditionPercents
            .filterKeys(segment.conditions::contains)
            .values
            .sum()
        return segment.tariffMoney * (1.0 + percent / 100.0)
    }

    return OvertimeBreakdown(
        overtimeSegments = overtimeSegments,
        halfRateSegments = halfRateSegments,
        fullRateSegments = fullRateSegments,
        ordinaryTariffMoney = overtimeSegments.sumOf(SalarySegment::tariffMoney),
        halfRateExtraMoney = halfRateSegments.sumOf(::applicableBaseMoney) * 0.5,
        fullRateExtraMoney = fullRateSegments.sumOf(::applicableBaseMoney),
    )
}

private fun requireNonOverlapping(segments: List<SalarySegment>) {
    segments.zipWithNext().forEach { (previous, next) ->
        require(previous.interval.endMillis <= next.interval.startMillis) {
            "Overtime segments must not overlap"
        }
    }
}
