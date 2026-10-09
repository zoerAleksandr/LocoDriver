package com.z_company.domain.autorecord

import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrainOrderingTest {
    private fun t(id: String, dep: Long? = null, arr: Long? = null) = Train(
        trainId = id,
        stations = if (dep == null && arr == null) mutableListOf()
        else mutableListOf(Station(timeDeparture = dep, timeArrival = arr)),
    )

    @Test
    fun `empty train wins over train with times`() {
        // В БД пустой поезд первым, а с временем — последним: старый баг писал в последний из БД.
        assertEquals("empty", TrainOrdering.pickAutoTarget(listOf(t("empty"), t("timed", dep = 100))).let { it?.trainId })
        assertEquals("empty", TrainOrdering.pickAutoTarget(listOf(t("timed", dep = 100), t("empty"))).let { it?.trainId })
    }

    @Test
    fun `all timed - last by display order`() {
        val r = TrainOrdering.pickAutoTarget(listOf(t("b", dep = 200), t("a", dep = 100)))
        assertEquals("b", r?.trainId)
    }

    @Test
    fun `timed trains without first departure keep db order after dated`() {
        val r = TrainOrdering.pickAutoTarget(listOf(t("x", arr = 50), t("a", dep = 100)))
        assertEquals("x", r?.trainId)
    }

    @Test
    fun `no trains gives null`() {
        assertNull(TrainOrdering.pickAutoTarget(emptyList()))
    }

    @Test
    fun `display sort - dated ascending then undated in input order`() {
        val sorted = TrainOrdering.sortForDisplay(listOf(t("u1"), t("b", dep = 200), t("u2"), t("a", dep = 100)))
        assertEquals(listOf("a", "b", "u1", "u2"), sorted.map { it.trainId })
    }

    @Test
    fun `preferred id from button wins over ordering rule`() {
        val trains = listOf(t("empty"), t("timed", dep = 100))
        assertEquals("timed", TrainOrdering.pickTarget(trains, "timed")?.trainId)
    }

    @Test
    fun `deleted or foreign preferred id falls back to ordering rule`() {
        val trains = listOf(t("timed", dep = 100), t("empty"))
        assertEquals("empty", TrainOrdering.pickTarget(trains, "gone")?.trainId)
    }

    @Test
    fun `no context uses ordering rule`() {
        val trains = listOf(t("a", dep = 100), t("b", dep = 200))
        assertEquals("b", TrainOrdering.pickTarget(trains, null)?.trainId)
        assertNull(TrainOrdering.pickTarget(emptyList(), null))
    }

    @Test
    fun `target in non-current route is found among all routes`() {
        val routes = listOf(listOf(t("cur")), listOf(t("other"), t("target")))
        assertEquals("target", TrainOrdering.findTargetTrain(routes, "target")?.trainId)
    }

    @Test
    fun `no target or deleted target - null so caller falls back to ordering`() {
        val routes = listOf(listOf(t("cur")))
        assertNull(TrainOrdering.findTargetTrain(routes, null))
        assertNull(TrainOrdering.findTargetTrain(routes, "gone"))
        assertEquals("cur", TrainOrdering.pickTarget(routes[0], null)?.trainId)
    }
}
