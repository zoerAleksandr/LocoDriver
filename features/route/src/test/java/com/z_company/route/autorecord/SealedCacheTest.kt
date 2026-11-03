package com.z_company.route.autorecord

import java.io.File
import javax.crypto.KeyGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SealedCacheTest {
    private fun seal(): LocalSeal {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        return LocalSeal { key }
    }

    @Test
    fun roundTripAndOtherKeyCannotOpen() {
        val a = seal()
        val blob = a.seal("{\"lat\":59.1234}".encodeToByteArray())
        assertEquals("{\"lat\":59.1234}", a.open(blob)!!.decodeToString())
        assertNull(seal().open(blob))
        assertNull(a.open(blob.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }))
        assertNull(a.open("{\"plain\":true}".encodeToByteArray()))
    }

    @Test
    fun cacheFilesAreNotReadableAsText() {
        val dir = createTempDir("sections")
        val seal = seal()
        val cache = FileSectionProfileCache(dir, seal)
        cache.save("s1", "{\"geometry\":[59.1234,30.5]}")
        cache.saveIndex("[{\"id\":\"s1\"}]")
        assertEquals("{\"geometry\":[59.1234,30.5]}", cache.load("s1"))
        assertEquals("[{\"id\":\"s1\"}]", cache.loadIndex())
        val raw = File(dir, "s1.json").readBytes().decodeToString()
        assertFalse(raw.contains("59.1234") || raw.contains("geometry"))
        // старый открытый файл (до шифрования) не читается - значит, будет перекачан
        File(dir, "old.json").writeText("{\"geometry\":[]}")
        assertNull(cache.load("old"))
        // чужой ключ (после сброса Keystore) - тоже перекачивание
        assertNull(FileSectionProfileCache(dir, seal()).load("s1"))
        dir.deleteRecursively()
    }

    @Test
    fun axisCacheRoundTripIsEncrypted() {
        val dir = createTempDir("axes")
        val cache = FileAxisCache(dir, seal())
        assertTrue(dir.listFiles().isNullOrEmpty())
        cache.save(com.z_company.domain.track.SectionAxis("ax-1", "h", listOf(com.z_company.domain.track.AxisPoint(0.0, 59.0, 30.0))))
        val raw = File(dir, "ax-1.axis").readBytes().decodeToString()
        assertFalse(raw.contains("59.0"))
        assertEquals("ax-1", cache.load("ax-1")!!.sectionId)
        dir.deleteRecursively()
    }
}
