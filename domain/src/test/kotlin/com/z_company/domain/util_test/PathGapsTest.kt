package com.z_company.domain.util_test

import com.z_company.domain.track.TrackPoint
import com.z_company.domain.util.PathGaps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PathGapsTest {
    private fun p(sec: Int) = TrackPoint(sec * 1000L, 55.0, 37.0, 60.0, 5f)

    @Test
    fun only_real_recording_gaps_count() {
        // ровная запись раз в 5 с, один настоящий разрыв 120 с, потом разрыв 6 мин (линию не рисуем вообще) - не «серый»
        val pts = listOf(p(0), p(5), p(10), p(130), p(135), p(500))
        assertEquals(listOf(10_000L to 130_000L), PathGaps.of(pts))
    }

    @Test
    fun simplified_neighbours_far_apart_are_not_a_gap() {
        val gaps = PathGaps.of(listOf(p(0), p(5), p(10), p(130), p(135)))
        // упрощённая линия: вершины на 0 с и на 100 с (середина прямой убрана) - разрыва записи между ними нет
        assertFalse(PathGaps.spans(gaps, 0, 8_000))
        assertFalse(PathGaps.spans(gaps, 20_000, 100_000))
        // а отрезок, накрывающий настоящий разрыв, - серый
        assertTrue(PathGaps.spans(gaps, 5_000, 135_000))
        assertTrue(PathGaps.spans(gaps, 10_000, 130_000))
    }

    @Test
    fun no_gaps_in_continuous_record() {
        assertTrue(PathGaps.of((0..100).map { p(it * 5) }).isEmpty())
    }
}
