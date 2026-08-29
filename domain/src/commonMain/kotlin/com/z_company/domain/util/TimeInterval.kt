package com.z_company.domain.util

/** Полуинтервал абсолютного времени [startMillis, endMillis). */
data class TimeInterval(
    val startMillis: Long,
    val endMillis: Long,
) {
    init {
        require(endMillis >= startMillis) { "Interval end must not precede start" }
    }

    val durationMillis: Long
        get() = endMillis - startMillis

    val isEmpty: Boolean
        get() = startMillis == endMillis

    fun intersect(other: TimeInterval): TimeInterval? {
        val start = maxOf(startMillis, other.startMillis)
        val end = minOf(endMillis, other.endMillis)
        return if (end > start) TimeInterval(start, end) else null
    }

    fun subtract(other: TimeInterval): List<TimeInterval> {
        val overlap = intersect(other) ?: return if (isEmpty) emptyList() else listOf(this)
        return buildList {
            if (startMillis < overlap.startMillis) add(TimeInterval(startMillis, overlap.startMillis))
            if (overlap.endMillis < endMillis) add(TimeInterval(overlap.endMillis, endMillis))
        }
    }

    fun splitAt(boundariesMillis: Iterable<Long>): List<TimeInterval> {
        if (isEmpty) return emptyList()
        val points = boundariesMillis
            .filter { it > startMillis && it < endMillis }
            .distinct()
            .sorted()
        return (listOf(startMillis) + points + endMillis)
            .zipWithNext { start, end -> TimeInterval(start, end) }
    }
}

fun Iterable<TimeInterval>.mergeTimeIntervals(): List<TimeInterval> {
    val sorted = filterNot(TimeInterval::isEmpty).sortedBy(TimeInterval::startMillis)
    if (sorted.isEmpty()) return emptyList()
    val result = mutableListOf(sorted.first())
    sorted.drop(1).forEach { interval ->
        val previous = result.last()
        if (interval.startMillis <= previous.endMillis) {
            result[result.lastIndex] = TimeInterval(
                previous.startMillis,
                maxOf(previous.endMillis, interval.endMillis),
            )
        } else {
            result += interval
        }
    }
    return result
}

fun TimeInterval.subtractAll(exclusions: Iterable<TimeInterval>): List<TimeInterval> =
    exclusions.mergeTimeIntervals().fold(listOf(this)) { remaining, exclusion ->
        remaining.flatMap { it.subtract(exclusion) }
    }
