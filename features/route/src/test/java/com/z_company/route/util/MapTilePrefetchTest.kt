package com.z_company.route.util

import com.z_company.domain.track.AxisPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapTilePrefetchTest {
    @Test
    fun tile_count_grows_with_length_and_zoom() {
        val short = MapTilePrefetch.estimateTileCount(
            listOf(AxisPoint(0.0, 55.0, 30.0), AxisPoint(1.0, 55.0, 30.05)),
        )
        val long = MapTilePrefetch.estimateTileCount(
            listOf(AxisPoint(0.0, 55.0, 30.0), AxisPoint(100.0, 55.5, 31.5)),
        )
        assertTrue(long > short)
        assertTrue(short > 0)
    }

    @Test
    fun empty_axis_has_no_tiles() {
        assertEquals(0, MapTilePrefetch.estimateTileCount(emptyList()))
        assertEquals(0, MapTilePrefetch.estimateTileCount(listOf(AxisPoint(0.0, 55.0, 30.0))))
    }

    @Test
    fun mb_estimate_is_small_for_short_section() {
        val count = MapTilePrefetch.estimateTileCount(
            listOf(AxisPoint(0.0, 55.0, 30.0), AxisPoint(10.0, 55.05, 30.1)),
        )
        val mb = MapTilePrefetch.estimateMb(count)
        assertTrue(mb in 0.2..60.0, "оценка двух тем $mb МБ для $count тайлов в разумных пределах")
    }

    @Test
    fun metadata_round_trip_preserves_atomic_pack_state() {
        val source = MapTilePrefetch.PackMetadata("section-1", "axis-2", "staging", 123L, MapTilePrefetch.STYLE_DARK)
        assertEquals(source, MapTilePrefetch.parseMetadata(MapTilePrefetch.encodeMetadata(source)))
    }

    @Test
    fun legacy_metadata_is_assigned_to_light_style() {
        val legacy = """{"sectionId":"section-1","axisHash":"axis-1"}""".encodeToByteArray()
        assertEquals(MapTilePrefetch.STYLE_LIGHT, MapTilePrefetch.parseMetadata(legacy)?.styleId)
    }

    @Test
    fun offline_package_is_complete_only_when_both_styles_exist() {
        val incomplete = OfflineTilePacks(mapOf("axis-1" to mapOf(MapTilePrefetch.STYLE_LIGHT to 1_000L)))
        val complete = OfflineTilePacks(
            mapOf("axis-1" to mapOf(MapTilePrefetch.STYLE_LIGHT to 1_000L, MapTilePrefetch.STYLE_DARK to 2_000L)),
        )

        assertNull(incomplete.bytesFor("axis-1"))
        assertEquals(3_000L, complete.bytesFor("axis-1"))
        assertEquals(3_000L, complete.totalBytes)
    }

    @Test
    fun legacy_metadata_is_ready_but_foreign_owner_is_ignored() {
        val legacy = """{"sectionId":"section-1","axisHash":"axis-1"}""".encodeToByteArray()
        assertEquals("ready", MapTilePrefetch.parseMetadata(legacy)?.state)
        assertEquals(MapTilePrefetch.STYLE_LIGHT, MapTilePrefetch.parseMetadata(legacy)?.styleId)

        val foreign = """{"owner":"another-app","sectionId":"section-1","axisHash":"axis-1","state":"ready"}"""
            .encodeToByteArray()
        assertNull(MapTilePrefetch.parseMetadata(foreign))
    }
}
