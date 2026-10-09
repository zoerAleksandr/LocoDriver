package com.z_company.route.autorecord

import com.z_company.domain.track.AltitudeSample
import com.z_company.domain.track.AxisPoint
import com.z_company.domain.track.PendingContribSession
import com.z_company.domain.track.SectionAxis
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AltitudeContribFilesTest {
    private fun tmp(): File = kotlin.io.path.createTempDirectory("altc").toFile()

    @Test
    fun storeRoundTripsAndDeletes() {
        val dir = tmp()
        val store = FileAltitudeContribStore(File(dir, "s"))
        val s = PendingContribSession("abc-123", 5L, listOf(AltitudeSample(55.5, 37.5, 100.0, null, 4f)))
        store.save(s)
        assertEquals(listOf("abc-123"), store.pendingIds())
        assertEquals(s.samples, store.load("abc-123")!!.samples)
        store.delete("abc-123")
        assertTrue(store.pendingIds().isEmpty())
        assertNull(store.load("abc-123"))
        // Небезопасный id (путь) игнорируется.
        assertNull(store.load("../x"))
        store.save(s)
        store.clear()
        assertTrue(store.pendingIds().isEmpty())
        dir.deleteRecursively()
    }

    @Test
    fun axisCacheKeepsOnlyListedSections() {
        val dir = tmp()
        val cache = FileAxisCache(File(dir, "a"))
        val pts = listOf(AxisPoint(0.0, 55.0, 37.0), AxisPoint(1.0, 55.01, 37.0))
        cache.save(SectionAxis("s1", "h1", pts))
        cache.save(SectionAxis("s2", "h2", pts))
        assertEquals("h1", cache.load("s1")!!.axisHash)
        cache.retain(setOf("s2"))
        assertNull(cache.load("s1"))
        assertNotNull(cache.load("s2"))
        dir.deleteRecursively()
    }
}
