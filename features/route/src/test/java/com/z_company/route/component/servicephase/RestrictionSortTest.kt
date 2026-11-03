package com.z_company.route.component.servicephase

import com.z_company.domain.track.RestrictionPass
import org.junit.Assert.assertEquals
import org.junit.Test

class RestrictionSortTest {
    private val order = listOf("Молосковицы", "Волосово", "Веймарн")

    private fun e(id: String, place: String, km: Double, dir: Int = 0, rel: Double = 0.0) = RestrictionPass(
        trainId = "t", warningId = id, routeId = "r", sectionId = if (dir != 0) "s" else RestrictionPass.NO_SECTION,
        kind = "speed", title = id, place = place, startAbsKm = km, endAbsKm = km + 0.1,
        startRelKm = rel, endRelKm = rel + 0.1, dir = dir, updatedAt = 1,
    )

    @Test
    fun kmGrowing_ascendingInsidePlace() {
        val list = listOf(e("b", "Волосово — Веймарн", 60.0), e("a", "Волосово — Веймарн", 55.0), e("c", "Молосковицы — Волосово", 40.0))
        assertEquals(listOf("c", "a", "b"), sortRestrictions(list, order).map { it.warningId })
    }

    @Test
    fun kmDecreasing_descendingInsidePlace() {
        val list = listOf(
            e("a", "Молосковицы — Волосово", 90.0), e("b", "Молосковицы — Волосово", 95.0),
            e("c", "Волосово — Веймарн", 80.0), e("d", "Волосово — Веймарн", 85.0),
        )
        assertEquals(listOf("b", "a", "d", "c"), sortRestrictions(list, order).map { it.warningId })
    }

    @Test
    fun singlePlace_directionFromPassedEntries() {
        // Км падает по ходу (dir = +1 по оси, км оси растёт, железнодорожный км падает).
        val list = listOf(
            e("a", "Волосово — Веймарн", 80.0, dir = 1, rel = 5.0).let { it.copy(endAbsKm = 79.9) },
            e("b", "Волосово — Веймарн", 85.0, dir = 1, rel = 3.0).let { it.copy(endAbsKm = 84.9) },
        )
        assertEquals(listOf("b", "a"), sortRestrictions(list, order).map { it.warningId })
    }
}
