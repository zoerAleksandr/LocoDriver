package com.z_company.domain.util

import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.LocomotiveSeriesTombstone
import com.z_company.domain.entities.route.LocoType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocomotiveSeriesMergeTest {
    private fun se(id: String, name: String, ts: Long, acc: Int? = null, type: LocoType = LocoType.ELECTRIC) =
        LocomotiveSeries(seriesId = id, name = name, type = type, acceptanceDurationMin = acc, updatedAt = ts)

    private fun tomb(id: String, name: String, ts: Long) =
        LocomotiveSeriesTombstone(id, name, LocoType.ELECTRIC, ts)

    @Test
    fun unionAndLww() {
        val r = LocomotiveSeriesMerge.merge(
            listOf(se("a", "ВЛ80", 1, acc = 10)), emptyList(),
            listOf(se("a", "ВЛ80", 5, acc = 20), se("b", "ЧС7", 1)), emptyList(),
        )
        assertEquals(setOf("a", "b"), r.series.map { it.seriesId }.toSet())
        assertEquals(20, r.series.first { it.seriesId == "a" }.acceptanceDurationMin)
    }

    @Test
    fun tombstoneBeatsOlderAndRevivesWhenNewer() {
        val dead = LocomotiveSeriesMerge.merge(listOf(se("a", "ВЛ80", 1)), emptyList(), emptyList(), listOf(tomb("a", "ВЛ80", 5)))
        assertTrue(dead.series.isEmpty())
        val alive = LocomotiveSeriesMerge.merge(listOf(se("a", "ВЛ80", 9)), emptyList(), emptyList(), listOf(tomb("a", "ВЛ80", 5)))
        assertEquals(1, alive.series.size)
    }

    @Test
    fun sameNameAndTypeCollapse_differentTypeKept() {
        val r = LocomotiveSeriesMerge.merge(
            listOf(se("a", "2ТЭ10", 1), se("c", "2ТЭ10", 1, type = LocoType.DIESEL)), emptyList(),
            listOf(se("b", "2тэ10", 2, acc = 30)), emptyList(),
        )
        assertEquals(setOf("b", "c"), r.series.map { it.seriesId }.toSet())
        assertEquals(listOf("a"), r.tombstones.map { it.seriesId })
    }

    @Test
    fun newerCopyWithoutBrakeFieldsInheritsThemFromOlder() {
        // Старый клиент правит нормы: сервер вернул свежую копию без паспортных полей - локальные не теряются.
        val local = se("a", "2ЭС6", 1).copy(brakeVolumeL = 2000, axleCount = 8, axleLoadTf = 14.0, weightT = 192.0, sectionCount = 2)
        val server = se("a", "2ЭС6", 9, acc = 25)
        val r = LocomotiveSeriesMerge.merge(listOf(local), emptyList(), listOf(server), emptyList()).series.single()
        assertEquals(9, r.updatedAt)
        assertEquals(25, r.acceptanceDurationMin)
        assertEquals(2000, r.brakeVolumeL)
        assertEquals(2, r.sectionCount)
        // и наоборот: локальная копия новее, а у сервера значения есть
        val r2 = LocomotiveSeriesMerge.merge(listOf(se("a", "2ЭС6", 9)), emptyList(), listOf(local), emptyList()).series.single()
        assertEquals(9, r2.updatedAt)
        assertEquals(8, r2.axleCount)
    }

    @Test
    fun newerCopyWithSomeBrakeFieldsKeepsItsOwnChoice() {
        val old = se("a", "ВЛ80С", 1).copy(brakeVolumeL = 1800, axleCount = 8, sectionCount = 1)
        val edited = se("a", "ВЛ80С", 5).copy(axleCount = 6)
        val r = LocomotiveSeriesMerge.merge(listOf(old), emptyList(), listOf(edited), emptyList()).series.single()
        assertEquals(6, r.axleCount)
        assertEquals(null, r.brakeVolumeL)
    }
}
