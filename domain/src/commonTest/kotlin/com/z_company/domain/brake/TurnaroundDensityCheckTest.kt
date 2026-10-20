package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class TurnaroundDensityCheckTest {
    private object V { val OK = TurnaroundDensityCheck.Verdict.OK; val EXCEEDED = TurnaroundDensityCheck.Verdict.EXCEEDED; val NO_REFERENCE = TurnaroundDensityCheck.Verdict.NO_REFERENCE }

    @Test fun exactlyTwentyPercentIsOk() {
        assertEquals(V.OK, TurnaroundDensityCheck.check(80.0, 100.0).verdict)
        assertEquals(V.OK, TurnaroundDensityCheck.check(120.0, 100.0).verdict)
    }

    @Test fun slightlyMoreThanTwentyPercentExceeded() {
        assertEquals(V.EXCEEDED, TurnaroundDensityCheck.check(79.9, 100.0).verdict)
        assertEquals(V.EXCEEDED, TurnaroundDensityCheck.check(120.1, 100.0).verdict)
    }

    @Test fun noReference() {
        val r = TurnaroundDensityCheck.check(90.0, null)
        assertEquals(V.NO_REFERENCE, r.verdict)
        assertNull(r.deviationPct)
        assertEquals(V.NO_REFERENCE, TurnaroundDensityCheck.check(90.0, 0.0).verdict)
    }

    @Test fun deviationSign() {
        assertEquals(-12.0, TurnaroundDensityCheck.check(88.0, 100.0).deviationPct!!, 1e-9)
        assertEquals(15.0, TurnaroundDensityCheck.check(115.0, 100.0).deviationPct!!, 1e-9)
        val r = TurnaroundDensityCheck.check(90.0, 100.0)
        assertEquals(80.0, r.lo!!, 1e-9); assertEquals(120.0, r.hi!!, 1e-9)
    }

    @Test fun compositionSummary() {
        assertNull(TurnaroundEntry("a").compositionSummary)
        // вес «ХХХХ т.», оси в скобках «(ХХХ осей)»
        assertEquals(
            "Новый вес 3100 т. (200 осей) · Потребное 1020 тс · Фактическое 1010 тс",
            TurnaroundEntry("a", newWeight = "3100", newAxles = "200", newRequired = "1020", newActual = "1010").compositionSummary,
        )
        assertEquals("Новый вес 3100 т.", TurnaroundEntry("a", newWeight = "3100").compositionSummary)
        assertEquals("Осей: (200 осей)", TurnaroundEntry("a", newAxles = "200").compositionSummary)
    }

    @Test fun kindShortAndOldRecords() {
        assertEquals("", TurnaroundEntry("a").testKindShort)
        assertEquals("ТП", TurnaroundEntry("a", testKind = TurnaroundEntry.KIND_FULL).testKindShort)
        assertEquals("СП", TurnaroundEntry("a", testKind = TurnaroundEntry.KIND_SHORT).testKindShort)
        val json = Json { ignoreUnknownKeys = true }
        // старая запись без поля testKind - вид пустой; со старым значением - читается как раньше
        assertEquals("", json.decodeFromString<TurnaroundEntry>("""{"id":"x"}""").testKind)
        val old = json.decodeFromString<TurnaroundEntry>("""{"id":"x","testKind":"сокращённое","densitySec":50.0}""")
        assertEquals("СП", old.testKindShort)
        assertNotNull(old.densitySec)
        assertTrue(old.testKind == "сокращённое")
    }
}
