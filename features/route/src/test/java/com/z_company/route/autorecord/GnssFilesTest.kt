package com.z_company.route.autorecord

import com.z_company.domain.gnss.GnssCell
import com.z_company.domain.gnss.GnssContribCell
import com.z_company.domain.gnss.GnssContribSession
import com.z_company.domain.gnss.GnssServerCell
import com.z_company.domain.gnss.GnssTileCache
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GnssFilesTest {
    private fun tmp(): File = kotlin.io.path.createTempDirectory("gnssf").toFile()

    @Test
    fun contribStoreRoundTripsDeletesAndRejectsUnsafeIds() {
        val dir = tmp()
        val store = FileGnssContribStore(File(dir, "c"))
        val s = GnssContribSession("abc-123", 5L, listOf(GnssContribCell(GnssCell(40133, 123456), 3, 2)))
        store.save(s)
        assertEquals(listOf("abc-123"), store.pendingIds())
        assertEquals(s.cells, store.load("abc-123")!!.cells)
        store.delete("abc-123")
        assertTrue(store.pendingIds().isEmpty())
        assertNull(store.load("../x"))
        // Пустой вклад на диск не пишется.
        store.save(GnssContribSession("empty", 1L, emptyList()))
        assertTrue(store.pendingIds().isEmpty())
        store.save(s)
        store.clear()
        assertTrue(store.pendingIds().isEmpty())
        dir.deleteRecursively()
    }

    @Test
    fun tileStoreRoundTripsAndClears() {
        val dir = tmp()
        val store = FileGnssTileStore(File(dir, "t"))
        val t = GnssTileCache("\"e\"", 9L, listOf(GnssServerCell(GnssCell(1, 2), 3, 5)))
        store.save("119_424", t)
        assertEquals(t, store.load("119_424"))
        assertNull(store.load("../../x"))
        store.clear()
        assertNull(store.load("119_424"))
        dir.deleteRecursively()
    }
}
