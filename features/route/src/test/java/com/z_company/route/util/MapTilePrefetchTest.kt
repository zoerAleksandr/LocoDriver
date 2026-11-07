package com.z_company.route.util

import com.z_company.domain.track.AxisPoint
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertTrue(mb in 0.1..30.0, "оценка $mb МБ для $count тайлов в разумных пределах")
    }
}
