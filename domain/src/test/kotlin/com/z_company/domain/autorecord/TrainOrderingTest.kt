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
}
