package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

/** Comparative microbenchmark for the station-tokenization hot path seen in Android ANR stacks. */
class StationNameMatcherPerformanceTest {
    private val pairs = listOf(
        "Гатчина ТБ" to "Гатчина-Товарная-Балтийская",
        "Санкт-Петербург-Глав." to "санкт петербург глав",
        "Ёлкино" to "елкино",
        "Узловая" to "Узловая II",
        "Лужская" to "Луга",
    )

    @Test
    fun cachedRegexMatchesBaselineAndReportsRelativeTiming() {
        repeat(2_000) { index ->
            val pair = pairs[index % pairs.size]
            baselineSameStation(pair.first, pair.second)
            StationNameMatcher.sameStation(pair.first, pair.second)
        }

        for ((left, right) in pairs) {
            assertEquals(baselineSameStation(left, right), StationNameMatcher.sameStation(left, right))
        }

        val iterations = 20_000
        var baselineMatches = 0
        val baselineTime = measureTime {
            repeat(iterations) { index ->
                val (left, right) = pairs[index % pairs.size]
                if (baselineSameStation(left, right)) baselineMatches++
            }
        }
        var optimizedMatches = 0
        val optimizedTime = measureTime {
            repeat(iterations) { index ->
                val (left, right) = pairs[index % pairs.size]
                if (StationNameMatcher.sameStation(left, right)) optimizedMatches++
            }
        }
        assertEquals(baselineMatches, optimizedMatches)
        println("StationNameMatcher benchmark: baseline=$baselineTime cachedRegex=$optimizedTime")
    }

    /** Mirrors the previous implementation, which compiled the split regex on every comparison. */
    private fun baselineSameStation(a: String, b: String): Boolean {
        val left = baselineWords(a)
        val right = baselineWords(b)
        if (left.isEmpty() || right.isEmpty()) return false
        if (baselineNorm(a) == baselineNorm(b)) return true
        if (baselineAbbreviation(left, right) || baselineAbbreviation(right, left)) return true
        return baselinePrefix(left, right) || baselinePrefix(right, left)
    }

    private fun baselineWords(s: String): List<String> = s.lowercase().replace('ё', 'е')
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotEmpty() }

    private fun baselineNorm(s: String) = s.lowercase().replace('ё', 'е').filter { it.isLetterOrDigit() }

    private fun baselinePrefix(short: List<String>, full: List<String>): Boolean {
        if (short.isEmpty() || full.size <= short.size || full.take(short.size) != short) return false
        return full.drop(short.size).all { it in setOf("i", "ii", "iii", "iv", "v", "vi", "1", "2", "3", "4", "5", "6") }
    }

    private fun baselineAbbreviation(short: List<String>, full: List<String>): Boolean {
        if (short.isEmpty() || short.size > full.size) return false
        var fullIndex = 0
        for (part in short) {
            if (part.length > 2) {
                if (fullIndex >= full.size || full[fullIndex] != part) return false
                fullIndex++
                continue
            }
            if (part.length == 1) {
                if (fullIndex >= full.size || full[fullIndex].firstOrNull() != part[0]) return false
                fullIndex++
                continue
            }
            if (fullIndex + part.length > full.size) return false
            val initials = full.subList(fullIndex, fullIndex + part.length).joinToString("") { it.first().toString() }
            if (initials != part) return false
            fullIndex += part.length
        }
        return fullIndex == full.size
    }
}
