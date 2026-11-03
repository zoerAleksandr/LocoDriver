package com.z_company.domain.util

import com.z_company.domain.gnss.GpsFixDiagnostics
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GpsFixDiagnosticsTest {
    @Test
    fun `gap is recorded with reasons and delivered count`() {
        val d = GpsFixDiagnostics()
        d.onDelivered(0)
        assertNull(d.onAccepted(0, 3f))
        repeat(5) { i ->
            d.onDelivered(1_000L + i)
            d.onRejected("NETWORK_MISMATCH")
        }
        d.onDelivered(121_000)
        val g = d.onAccepted(121_000, 3f)
        assertNotNull(g)
        assertEquals(121_000L, g.durationMs)
        assertEquals(6, g.delivered)
        assertEquals(mapOf("NETWORK_MISMATCH" to 5), g.rejected)
        assertEquals("G", g.accuracyClass)
        assertEquals(121_000L, d.maxGapMs)
        assertEquals("t=0;dur=121;deliv=6;acc=G;rej=NETWORK_MISMATCH:5", g.serialize())
        assertEquals(mapOf("NETWORK_MISMATCH" to 5), d.rejectedTotals())
    }

    @Test
    fun `silent system gives zero delivered and short pauses are not gaps`() {
        val d = GpsFixDiagnostics()
        d.onAccepted(0, 40f)
        assertNull(d.onAccepted(19_000, 4f))
        val g = d.onAccepted(60_000, 40f)!!
        assertEquals(0, g.delivered)
        assertEquals("B", g.accuracyClass)
    }

    @Test
    fun `gap ring is bounded`() {
        val d = GpsFixDiagnostics(maxGaps = 3)
        var t = 0L
        repeat(10) {
            d.onAccepted(t, 3f)
            t += 30_000
        }
        assertEquals(3, d.recentGaps().size)
        assertTrue(d.recentGaps().all { it.durationMs == 30_000L })
    }
}
